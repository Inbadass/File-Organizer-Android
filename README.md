# File Organizer — Android

An Android file organizer evolved from my original Python CLI File Organizer into a native Android application.

The project automatically categorizes files based on their extensions, moves them into organized folders, records successful operations using JSON, and provides an undo feature.

## Features

### V2 — Android File Organizer

* Organize files based on their extensions
* Categorize files into folders such as Images, Video, Audio, Documents, Python, and Kotlin
* Move files into their corresponding category folders
* Automatically create category folders when required
* Skip files when a file with the same name already exists at the destination
* Persistent operation history using JSON
* Undo the previous organizing operation
* Restore files to their original locations
* Handle storage permissions
* Support source and destination subfolder paths
* Display operation logs inside the application
* Display the number of files moved after an organizing operation
* Handle common file-system errors
* Use `renameTo()` when possible, with copy-and-delete as a fallback

## File Categories

| Extension               | Category  |
| ----------------------- | --------- |
| `.jpg`, `.jpeg`, `.png` | Images    |
| `.mp4`                  | Video     |
| `.mp3`                  | Audio     |
| `.pdf`, `.txt`, `.json` | Documents |
| `.py`                   | Python    |
| `.kt`                   | Kotlin    |
| Other extensions        | Others    |

## Undo & History

The application maintains a `history.json` file inside its internal application storage.

Each successful file movement is recorded with its original and new path:

```json
{
    "from": "source/file.txt",
    "to": "destination/Documents/file.txt"
}
```

When `Undo` is selected, the application reads the stored history and attempts to move each file back to its original location.

Successfully restored operations are removed from the history.

If an operation fails, its history entry is retained so that it can be attempted again.

## Storage Permission

The application requires access to external storage in order to organize files outside its private application directory.

On Android versions supporting broad external-storage access, the application checks whether the required storage access has been granted and provides a button to open the appropriate Android settings page.

For older Android versions, the application requests the required read and write storage permissions.

## How It Works

1. The application checks storage permission.
2. The user enters the source folder.
3. The user enters the destination folder.
4. The application scans the source folder.
5. Each file is classified using its extension.
6. The required category folder is created inside the destination folder.
7. The application checks whether a file with the same name already exists.
8. If it does, the file is skipped.
9. Otherwise, the file is moved to its category folder.
10. The successful operation is recorded in the history.
11. The application displays the operation result.
12. The `Undo` option can restore the recorded file movements.

## File Moving

The application first attempts to move a file using:

```text
File.renameTo()
```

If that fails, it falls back to:

```text
copy → delete
```

This allows the application to handle situations where a direct rename/move operation is not available.

## Technologies

### V2 — Android

* Kotlin
* Android SDK
* Android Storage APIs
* `File`
* `Environment`
* `Intent`
* `org.json.JSONArray`
* `org.json.JSONObject`
* Threads
* Android UI widgets

### V1 — Python CLI

* Python
* `pathlib`
* `shutil`
* `json`
* `logging`

## Versions

### V1 — Python CLI

The original version of the project is a command-line file organizer written in Python.

V1 supports:

* Automatic file organization
* Extension-based categorization
* File movement using `shutil`
* Duplicate-file skipping
* Persistent operation history using JSON
* Undo functionality
* Operation logging
* Operation summaries
* File-system error handling

V1 runs entirely from the command line.

### V2 — Android Application

V2 moves the project from a Python command-line application to a native Android application.

It adds:

* Android user interface
* Android storage permission handling
* Source and destination folder inputs
* Persistent JSON history inside the application
* File restoration through Undo
* On-screen operation logs
* Android-specific file handling

The core idea remains the same:

```text
Select source
      ↓
Scan files
      ↓
Classify by extension
      ↓
Create category folder
      ↓
Move files
      ↓
Record operation
      ↓
Undo if required
```

## Version Status

| Version | Status    | Main Focus                             |
| ------- | --------- | -------------------------------------- |
| `V1`    | Completed | Python CLI + File Organization + Undo  |
| `V2`    | Completed | Android App + File Organization + Undo |

## Project Evolution

This project was originally developed as a Python CLI application.

After completing the CLI version, the same core idea was rebuilt as an Android application.

The two versions use different platforms and APIs, but solve the same problem:

```text
V1
Python CLI
    ↓
File Organization
    ↓
JSON History
    ↓
Undo

        ↓ Evolution ↓

V2
Android Application
    ↓
Android UI
    ↓
File Organization
    ↓
JSON History
    ↓
Undo
```

The Android version is therefore not a completely separate project, but the second iteration of the original File Organizer.

## Future Improvements

* Improved user interface
* More file categories
* More configurable file-extension mappings
* Improved operation history management
* More detailed operation statistics

## License

This project is intended for learning and personal development.
