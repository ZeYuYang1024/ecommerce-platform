"""Generate placeholder images for the ecommerce mini-program."""
from PIL import Image, ImageDraw, ImageFont
import os, math

OUT = "ecommerce-miniprogram/static"
os.makedirs(OUT, exist_ok=True)

# Color palette
AMBER = "#F59E0B"
AMBER_DARK = "#D97706"
ORANGE = "#F97316"
WHITE = "#FFFFFF"
LIGHT_BG = "#F8F8F8"
DARK_TEXT = "#1F2937"
GRAY = "#9CA3AF"
RED = "#EF4444"
GREEN = "#10B981"

def solid_bg(size, color, name):
    img = Image.new("RGB", size, color)
    img.save(f"{OUT}/{name}")
    print(f"  {name} ({size[0]}x{size[1]})")

def gradient_bg(size, color1, color2, name):
    img = Image.new("RGB", size)
    for y in range(size[1]):
        r = int(int(color1[1:3], 16) + (int(color2[1:3], 16) - int(color1[1:3], 16)) * y / size[1])
        g = int(int(color1[3:5], 16) + (int(color2[3:5], 16) - int(color1[3:5], 16)) * y / size[1])
        b = int(int(color1[5:7], 16) + (int(color2[5:7], 16) - int(color1[5:7], 16)) * y / size[1])
        for x in range(size[0]):
            img.putpixel((x, y), (r, g, b))
    img.save(f"{OUT}/{name}")
    print(f"  {name} ({size[0]}x{size[1]})")

def banner(text, name, bg_color=AMBER, text_color=WHITE):
    size = (750, 300)
    img = Image.new("RGB", size, bg_color)
    draw = ImageDraw.Draw(img)
    # Decorative circles
    for cx, cy, r, op in [(600, 80, 120, 0.1), (100, 220, 80, 0.08), (680, 260, 60, 0.12)]:
        draw.ellipse([cx-r, cy-r, cx+r, cy+r], fill=WHITE, outline=None)
    # Text
    try:
        f = ImageFont.truetype("arial.ttf", 42)
        f2 = ImageFont.truetype("arial.ttf", 20)
    except:
        f = ImageFont.load_default()
        f2 = ImageFont.load_default()
    bbox = draw.textbbox((0, 0), text, font=f)
    tw = bbox[2] - bbox[0]
    draw.text(((size[0]-tw)//2, size[1]//2 - 30), text, fill=text_color, font=f)
    sub = "限时特惠 品质好物"
    bbox2 = draw.textbbox((0, 0), sub, font=f2)
    sw = bbox2[2] - bbox2[0]
    draw.text(((size[0]-sw)//2, size[1]//2 + 15), sub, fill=text_color+"CC", font=f2)
    img.save(f"{OUT}/{name}")
    print(f"  {name} ({size[0]}x{size[1]})")

def product_card(text, name, bg=LIGHT_BG):
    size = (400, 400)
    img = Image.new("RGB", size, bg)
    draw = ImageDraw.Draw(img)
    # Product area (top 70%)
    draw.rectangle([20, 20, 380, 280], fill=WHITE, outline="#E5E7EB")
    draw.text((200, 140), text[:2] if text else "?", fill=GRAY, anchor="mm",
              font=ImageFont.truetype("arial.ttf", 48) if os.path.exists("arial.ttf") else ImageFont.load_default())
    # Price area
    try: f = ImageFont.truetype("arial.ttf", 28)
    except: f = ImageFont.load_default()
    draw.text((30, 310), "¥99.00", fill=RED, font=f)
    draw.text((30, 345), text[:12] if text else "商品名称", fill=DARK_TEXT,
              font=ImageFont.truetype("arial.ttf", 20) if os.path.exists("arial.ttf") else ImageFont.load_default())
    # Badge
    draw.rectangle([280, 305, 370, 335], fill=AMBER+"20", outline=AMBER)
    draw.text((325, 320), "热卖", fill=AMBER, anchor="mm",
              font=ImageFont.truetype("arial.ttf", 14) if os.path.exists("arial.ttf") else ImageFont.load_default())
    img.save(f"{OUT}/{name}")
    print(f"  {name} ({size[0]}x{size[1]})")

def tab_icon(shape, name, color=DARK_TEXT, active=False):
    size = (48, 48)
    img = Image.new("RGBA", size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    c = AMBER if active else color
    if shape == "home":
        draw.polygon([(24,6),(6,22),(14,22),(14,42),(34,42),(34,22),(42,22)], fill=c)
    elif shape == "grid":
        for i in range(4):
            x, y = 8 + (i%2)*17, 8 + (i//2)*17
            draw.rounded_rectangle([x, y, x+14, y+14], radius=3, fill=c)
    elif shape == "cart":
        draw.ellipse([30, 32, 42, 44], fill=c)
        draw.polygon([(8,10),(14,36),(40,36),(42,10)], fill=c)
    elif shape == "user":
        draw.ellipse([14, 6, 34, 26], fill=c)
        draw.ellipse([4, 30, 44, 48], fill=c)
    img.save(f"{OUT}/{name}")
    print(f"  {name}")

def seckill_card(text, name):
    size = (500, 320)
    img = Image.new("RGB", size, WHITE)
    draw = ImageDraw.Draw(img)
    # Top timer bar
    draw.rectangle([0, 0, 500, 40], fill="#1F2937")
    try: f = ImageFont.truetype("arial.ttf", 18)
    except: f = ImageFont.load_default()
    draw.text((20, 10), "⏱ 距结束 02:30:15", fill=WHITE, font=f)
    # Product image area
    draw.rectangle([16, 56, 200, 280], fill=LIGHT_BG, outline="#E5E7EB")
    draw.text((108, 168), text[:2] if text else "?", fill=GRAY, anchor="mm",
              font=ImageFont.truetype("arial.ttf", 40) if os.path.exists("arial.ttf") else ImageFont.load_default())
    # Info
    try: f2 = ImageFont.truetype("arial.ttf", 24)
    except: f2 = ImageFont.load_default()
    draw.text((220, 60), text[:15] if text else "秒杀商品", fill=DARK_TEXT, font=f2)
    draw.text((220, 100), "¥1999", fill=RED, font=ImageFont.truetype("arial.ttf", 32) if os.path.exists("arial.ttf") else ImageFont.load_default())
    draw.text((320, 108), "¥2999", fill=GRAY, font=ImageFont.truetype("arial.ttf", 18) if os.path.exists("arial.ttf") else ImageFont.load_default())
    # Progress bar
    draw.rounded_rectangle([220, 200, 470, 210], radius=5, fill="#E5E7EB")
    draw.rounded_rectangle([220, 200, 370, 210], radius=5, fill=RED)
    draw.text((220, 220), "已抢 60%", fill=RED,
              font=ImageFont.truetype("arial.ttf", 14) if os.path.exists("arial.ttf") else ImageFont.load_default())
    # Button
    draw.rounded_rectangle([220, 248, 470, 284], radius=8, fill=RED)
    draw.text((345, 266), "立即秒杀", fill=WHITE, anchor="mm",
              font=ImageFont.truetype("arial.ttf", 18) if os.path.exists("arial.ttf") else ImageFont.load_default())
    img.save(f"{OUT}/{name}")
    print(f"  {name} ({size[0]}x{size[1]})")

def coupon_card(name, out_name):
    size = (500, 140)
    img = Image.new("RGB", size, WHITE)
    draw = ImageDraw.Draw(img)
    # Left: amount
    draw.rectangle([0, 0, 150, 140], fill=AMBER+"20")
    try: f = ImageFont.truetype("arial.ttf", 32)
    except: f = ImageFont.load_default()
    draw.text((75, 50), "¥20", fill=AMBER, anchor="mm", font=f)
    draw.text((75, 85), "满100可用", fill=GRAY, anchor="mm",
              font=ImageFont.truetype("arial.ttf", 14) if os.path.exists("arial.ttf") else ImageFont.load_default())
    # Right: info
    try: f2 = ImageFont.truetype("arial.ttf", 20)
    except: f2 = ImageFont.load_default()
    draw.text((175, 30), name[:15] if name else "满减券", fill=DARK_TEXT, font=f2)
    draw.text((175, 60), "有效期至 2026-12-31", fill=GRAY,
              font=ImageFont.truetype("arial.ttf", 14) if os.path.exists("arial.ttf") else ImageFont.load_default())
    # Claim button
    draw.rounded_rectangle([380, 50, 470, 90], radius=8, fill=AMBER)
    draw.text((425, 70), "领取", fill=WHITE, anchor="mm",
              font=ImageFont.truetype("arial.ttf", 14) if os.path.exists("arial.ttf") else ImageFont.load_default())
    # Dashed line
    for y in range(0, 140, 8):
        draw.rectangle([150, y, 152, y+4], fill="#E5E7EB")
    img.save(f"{OUT}/{out_name}")
    print(f"  {out_name} ({size[0]}x{size[1]})")

def empty_state(text, name):
    size = (400, 300)
    img = Image.new("RGB", size, WHITE)
    draw = ImageDraw.Draw(img)
    draw.ellipse([150, 60, 250, 160], fill=LIGHT_BG, outline="#E5E7EB")
    try: f = ImageFont.truetype("arial.ttf", 18)
    except: f = ImageFont.load_default()
    draw.text((200, 200), text, fill=GRAY, anchor="mm", font=f)
    img.save(f"{OUT}/{name}")
    print(f"  {name} ({size[0]}x{size[1]})")

# ========== Generate ==========
print("Generating images...")
print("  Banners:")
banner("品质好物 一站购齐", "banner_hero.png", bg_color=AMBER)
banner("限时秒杀 手慢无", "banner_seckill.png", bg_color=RED)
banner("新品上市", "banner_new.png", bg_color=AMBER_DARK)

print("  Products:")
product_card("时尚运动鞋", "product_01.png")
product_card("轻薄羽绒服", "product_02.png")
product_card("无线蓝牙耳机", "product_03.png")
product_card("智能手表", "product_04.png")
product_card("有机绿茶礼盒", "product_05.png")
product_card("天然护肤套装", "product_06.png")

print("  Tab icons:")
for shape in ["home", "grid", "cart", "user"]:
    tab_icon(shape, f"tab_{shape}.png")
    tab_icon(shape, f"tab_{shape}_active.png", active=True)

print("  Seckill & Coupon:")
seckill_card("秒杀旗舰手机", "seckill_card.png")
coupon_card("满100减20券", "coupon_card.png")

print("  Empty states:")
empty_state("购物车空空如也", "empty_cart.png")
empty_state("暂无订单", "empty_order.png")
empty_state("暂无优惠券", "empty_coupon.png")

print(f"\nAll images saved to {OUT}/")
