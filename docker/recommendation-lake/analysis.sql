CREATE TEMP TABLE engagement AS
SELECT user_id, spu_id, category_id, event_type, CAST(occurred_at AS DATE) AS event_date,
       MAX(occurred_at) AS last_interaction,
       CASE event_type WHEN 'PAID' THEN 6 WHEN 'CART_ADD' THEN 4 ELSE 1 END AS weight
FROM lake_events
WHERE event_type IN ('VIEW', 'CART_ADD', 'PAID')
  AND occurred_at >= CURRENT_TIMESTAMP - INTERVAL 30 DAY
GROUP BY user_id, spu_id, category_id, event_type, CAST(occurred_at AS DATE);

CREATE TEMP TABLE user_category AS
SELECT user_id, category_id,
       SUM(CASE WHEN last_interaction >= CURRENT_TIMESTAMP - INTERVAL 7 DAY THEN weight ELSE 0 END) AS score_7d,
       SUM(weight) AS score_30d
FROM engagement
WHERE category_id IS NOT NULL
GROUP BY user_id, category_id;

CREATE TEMP TABLE user_product AS
SELECT user_id, spu_id, last_interaction
FROM (
    SELECT user_id, spu_id, MAX(last_interaction) AS last_interaction
    FROM engagement GROUP BY user_id, spu_id
) latest
QUALIFY ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY last_interaction DESC, spu_id) <= 30;

CREATE TEMP TABLE related_product AS
WITH audience AS (
    SELECT spu_id, COUNT(*) AS users FROM user_product GROUP BY spu_id
),
pairs AS (
    SELECT a.spu_id, b.spu_id AS related_spu_id, COUNT(*) AS shared_users
    FROM user_product a JOIN user_product b ON a.user_id = b.user_id AND a.spu_id <> b.spu_id
    GROUP BY a.spu_id, b.spu_id
    HAVING COUNT(*) >= 2
)
SELECT pairs.spu_id, related_spu_id, shared_users,
       shared_users / SQRT(CAST(a.users AS DOUBLE) * b.users) AS affinity
FROM pairs
JOIN audience a ON pairs.spu_id = a.spu_id
JOIN audience b ON related_spu_id = b.spu_id
QUALIFY ROW_NUMBER() OVER (PARTITION BY pairs.spu_id ORDER BY affinity DESC, shared_users DESC, related_spu_id) <= 20;

CREATE TEMP TABLE recommendation_impressions AS
SELECT impression.event_id, impression.user_id, impression.spu_id, impression.request_id,
       CAST(impression.occurred_at AS DATE) AS event_date,
       COALESCE(impression.surface, 'WEB') AS surface,
       COALESCE(impression.strategy, 'DEFAULT') AS strategy,
       MAX(clicked.occurred_at) AS clicked_at
FROM lake_events impression
LEFT JOIN lake_events clicked ON clicked.event_type = 'CLICK'
    AND impression.user_id = clicked.user_id AND impression.request_id = clicked.request_id
    AND impression.spu_id = clicked.spu_id AND clicked.occurred_at >= impression.occurred_at
    AND clicked.occurred_at <= impression.occurred_at + INTERVAL 30 MINUTE
WHERE impression.event_type = 'IMPRESSION' AND impression.occurred_at >= CURRENT_TIMESTAMP - INTERVAL 30 DAY
GROUP BY 1, 2, 3, 4, 5, 6, 7;

CREATE TEMP TABLE daily_report AS
SELECT event_date, COUNT(*) AS impressions, COUNT(clicked_at) AS clicks,
       CAST(COUNT(clicked_at) AS DOUBLE) / COUNT(*) AS click_rate
FROM recommendation_impressions GROUP BY event_date;

CREATE TEMP TABLE funnel_report AS
WITH last_touch AS (
    SELECT impression.event_id AS impression_id, conversion.event_type
    FROM lake_events conversion
    JOIN recommendation_impressions impression
      ON conversion.user_id = impression.user_id AND conversion.spu_id = impression.spu_id
      AND conversion.occurred_at >= impression.clicked_at
      AND conversion.occurred_at <= impression.clicked_at
          + CASE conversion.event_type WHEN 'PAID' THEN INTERVAL 24 HOUR ELSE INTERVAL 30 MINUTE END
    WHERE conversion.event_type IN ('CART_ADD', 'PAID')
    QUALIFY ROW_NUMBER() OVER (
        PARTITION BY conversion.event_id ORDER BY impression.clicked_at DESC, impression.request_id DESC
    ) = 1
), stages AS (
    SELECT impression_id, BOOL_OR(event_type = 'CART_ADD') AS added, BOOL_OR(event_type = 'PAID') AS paid
    FROM last_touch GROUP BY impression_id
)
SELECT event_date, surface, strategy, COUNT(*) AS impressions, COUNT(clicked_at) AS clicks,
       COUNT(*) FILTER (WHERE stages.added) AS cart_adds, COUNT(*) FILTER (WHERE stages.paid) AS payments
FROM recommendation_impressions
LEFT JOIN stages ON recommendation_impressions.event_id = stages.impression_id
GROUP BY event_date, surface, strategy;
