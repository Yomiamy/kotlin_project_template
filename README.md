# kotlin_project_template

Kotlin project template used in any kick-off project to reduce the setup duration.

Based on Android Architecture Starter Template (single module).

This template is compatible with the latest **stable** version of Android Studio.

## Screenshots
![Screenshot](https://github.com/android/architecture-templates/raw/main/screenshots.png)

## Features

* Room Database
* Hilt
* ViewModel, read+write
* UI in Compose, list + write (Material3)
* Navigation
* Repository and data source
* Kotlin Coroutines and Flow
* Unit tests
* UI tests using fake data with Hilt

## Usage

Run the customizer script:

```bash
./customizer.sh your.package.name DataItemType [MyApplication]
```

Where `your.package.name` is your app ID (should be lowercase) and `DataItemType` is used for the
name of the screen, exposed state and data base entity (should be PascalCase). You can add an optional application name.

# License

Now in Android is distributed under the terms of the Apache License (Version 2.0). See the
[license](LICENSE) for more information.
