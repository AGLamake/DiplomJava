"""Вырезает схемы автомобиля и квартиры из скриншотов диплома и стирает старые подписи датчиков.

Запуск: python -I tools/make_scheme_images.py <путь к docx> <папка для png>
"""
import io
import sys
import zipfile

from PIL import Image, ImageDraw

CROP = (125, 80, 717, 435)

# Координаты центров датчиков относительно обрезки и подписи (как в SensorPoint.java)
CAR = [("Капот", 58, 85), ("Передняя дверь", 332, 99), ("Задняя дверь", 438, 123),
       ("Багажник", 58, 259), ("Зажигание", 318, 273), ("АКБ", 254, 302)]
NDV = [("ДИП", 98, 46), ("ДУГ", 406, 61), ("ИК", 18, 144), ("СМК", 303, 178),
       ("ДРС", 506, 160), ("ДУВ", 506, 207), ("КТС", 177, 259)]


def make(docx: zipfile.ZipFile, member: str, points, out_path: str) -> None:
    img = Image.open(io.BytesIO(docx.read(member))).convert("RGB").crop(CROP)
    draw = ImageDraw.Draw(img)
    for label, x, y in points:
        draw.rectangle((x - 12, y - 12, x + 20 + 9 * len(label), y + 13), fill="white")
    img.save(out_path)


def main() -> None:
    docx_path, out_dir = sys.argv[1], sys.argv[2]
    with zipfile.ZipFile(docx_path) as docx:
        make(docx, "word/media/image22.png", CAR, out_dir + "/car.png")
        make(docx, "word/media/image23.png", NDV, out_dir + "/flat.png")


if __name__ == "__main__":
    main()
