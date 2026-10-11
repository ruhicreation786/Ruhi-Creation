"""
Find every photo of one person in a folder and move them into a subfolder.

Runs entirely on your own computer - no photos are uploaded anywhere.

Default is PREVIEW mode: it only lists the matches. Add --move to actually move them.

Examples (Windows Command Prompt):
    python find_her_photos.py --reference reference.jpg
    python find_her_photos.py --reference reference.jpg --move
    python find_her_photos.py --undo "D:\\S21 FE Backup\\Pictures\\Her\\_moved_log.csv"
"""

import argparse
import csv
import shutil
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageOps

import face_recognition

try:
    from pillow_heif import register_heif_opener

    register_heif_opener()
    HEIC_SUPPORTED = True
except ImportError:
    HEIC_SUPPORTED = False

DEFAULT_SOURCE = r"D:\S21 FE Backup\Pictures"
DEFAULT_FOLDER_NAME = "Her"
LOG_NAME = "_moved_log.csv"
IMAGE_EXTS = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}
if HEIC_SUPPORTED:
    IMAGE_EXTS |= {".heic", ".heif"}

# Phone photos are ~4000px wide; shrinking them makes face detection much faster.
MAX_SIDE = 1600


def load_image(path):
    """Load an image as an RGB numpy array, respecting phone rotation (EXIF)."""
    with Image.open(path) as img:
        img = ImageOps.exif_transpose(img).convert("RGB")
        img.thumbnail((MAX_SIDE, MAX_SIDE))
        return np.array(img)


def reference_encodings(paths):
    encodings = []
    for p in paths:
        faces = face_recognition.face_encodings(load_image(p))
        if not faces:
            print(f"WARNING: no face found in reference photo {p}")
        elif len(faces) > 1:
            print(f"WARNING: {p} has {len(faces)} faces; use a photo with only her in it.")
        else:
            encodings.append(faces[0])
    if not encodings:
        sys.exit("No usable reference face. Use a clear, front-facing photo of only her.")
    return encodings


def find_images(source, dest, recursive):
    pattern = "**/*" if recursive else "*"
    for p in sorted(source.glob(pattern)):
        if p.is_file() and p.suffix.lower() in IMAGE_EXTS and dest not in p.parents:
            yield p


def unique_target(dest, name):
    target = dest / name
    n = 1
    while target.exists():
        target = dest / f"{Path(name).stem}_{n}{Path(name).suffix}"
        n += 1
    return target


def undo(log_path):
    log_path = Path(log_path)
    with open(log_path, newline="", encoding="utf-8") as f:
        rows = list(csv.DictReader(f))
    restored = 0
    for row in rows:
        src, dst = Path(row["moved_to"]), Path(row["original"])
        if src.exists() and not dst.exists():
            shutil.move(str(src), str(dst))
            restored += 1
        else:
            print(f"Skipped (missing or already exists): {src}")
    print(f"Restored {restored} of {len(rows)} photos.")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--reference", nargs="+", help="Photo(s) of her. More photos = better accuracy.")
    ap.add_argument("--source", default=DEFAULT_SOURCE, help=f"Folder to search (default: {DEFAULT_SOURCE})")
    ap.add_argument("--folder-name", default=DEFAULT_FOLDER_NAME, help="Name of the new folder (default: Her)")
    ap.add_argument("--tolerance", type=float, default=0.5,
                    help="Lower = stricter. 0.5 is a good start; try 0.45 if you get wrong matches, 0.55 if you miss some.")
    ap.add_argument("--recursive", action="store_true", help="Also search subfolders.")
    ap.add_argument("--move", action="store_true", help="Actually move the files (otherwise only preview).")
    ap.add_argument("--undo", metavar="LOG_CSV", help="Move photos back using the log file.")
    args = ap.parse_args()

    if args.undo:
        undo(args.undo)
        return
    if not args.reference:
        ap.error("--reference is required")

    source = Path(args.source)
    if not source.is_dir():
        sys.exit(f"Folder not found: {source}")
    dest = source / args.folder_name

    known = reference_encodings(args.reference)
    images = list(find_images(source, dest, args.recursive))
    print(f"Checking {len(images)} photos in {source} ...")
    if not HEIC_SUPPORTED:
        print("(HEIC photos are skipped - run 'pip install pillow-heif' to include them.)")

    matches = []
    for i, path in enumerate(images, 1):
        try:
            faces = face_recognition.face_encodings(load_image(path))
        except Exception as e:
            print(f"  [{i}/{len(images)}] could not read {path.name}: {e}")
            continue
        best = min((face_recognition.face_distance(known, f).min() for f in faces), default=None)
        if best is not None and best <= args.tolerance:
            matches.append((path, best))
            print(f"  [{i}/{len(images)}] MATCH {path.name}  (distance {best:.2f})")
        elif i % 50 == 0:
            print(f"  [{i}/{len(images)}] ...")

    print(f"\nFound {len(matches)} photo(s) of her.")
    if not matches:
        return
    if not args.move:
        print("PREVIEW only - nothing was moved. Re-run with --move to move them.")
        return

    dest.mkdir(exist_ok=True)
    log_path = dest / LOG_NAME
    write_header = not log_path.exists()
    with open(log_path, "a", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        if write_header:
            writer.writerow(["original", "moved_to", "distance"])
        for path, dist in matches:
            target = unique_target(dest, path.name)
            shutil.move(str(path), str(target))
            writer.writerow([str(path), str(target), f"{dist:.3f}"])
    print(f"Moved {len(matches)} photo(s) to {dest}")
    print(f'To undo: python find_her_photos.py --undo "{log_path}"')


if __name__ == "__main__":
    main()
