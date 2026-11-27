FROM python:3.15.2-slim-bookworm

ENV PYTHONUNBUFFERED=1 PYTHONDONTWRITEBYTECODE=1 HOME=/home/lake
WORKDIR /app
RUN useradd --create-home --uid 10001 lake
COPY requirements.txt .
RUN pip install --no-cache-dir --only-binary=:all: -r requirements.txt
USER lake
RUN python -c "import duckdb; duckdb.connect().execute('INSTALL httpfs; LOAD httpfs')"
COPY worker.py analysis.sql ./
ENTRYPOINT ["python", "/app/worker.py"]
