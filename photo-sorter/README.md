# Photo sorter

Finds every photo of one person in `D:\S21 FE Backup\Pictures` and moves them into a new folder, `D:\S21 FE Backup\Pictures\Her`. It runs only on your PC, so no photos are uploaded.

## One-time setup (Windows)

1. Install Python 3.11 or 3.12 from https://www.python.org/downloads/ and tick **"Add Python to PATH"** during install.
2. Open **Command Prompt** and run:

   ```
   pip install dlib-bin numpy Pillow pillow-heif
   pip install face_recognition --no-deps
   pip install face_recognition_models Click "setuptools<81"
   ```

   (`dlib-bin` is a prebuilt version of dlib, so you don't need Visual Studio or CMake.)

## Use it

1. Put `find_her_photos.py` and her reference photo (for example `reference.jpg`) in the same folder, such as `C:\photo-sorter`.
2. Open Command Prompt in that folder: `cd C:\photo-sorter`
3. **Preview** (moves nothing, only lists the matches):

   ```
   python find_her_photos.py --reference reference.jpg
   ```

4. If the list looks right, **move** the photos:

   ```
   python find_her_photos.py --reference reference.jpg --move
   ```

## Tips

- **Better accuracy:** pass several clear photos of her, for example `--reference ref1.jpg ref2.jpg ref3.jpg`.
- **Wrong people matched:** add `--tolerance 0.45`.
- **Some of her photos missed:** add `--tolerance 0.55`.
- **Search subfolders too:** add `--recursive`.
- **Undo:** each move is recorded in `Her\_moved_log.csv`. To put every photo back where it was, run:

  ```
  python find_her_photos.py --undo "D:\S21 FE Backup\Pictures\Her\_moved_log.csv"
  ```
