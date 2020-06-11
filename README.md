# Arclight

A Bukkit server implementation utilizing Mixin.

[![AppVeyor](https://img.shields.io/appveyor/build/IzzelAliz/arclight?style=flat-square)](https://ci.appveyor.com/project/IzzelAliz/arclight) ![MC 1.14.4](https://img.shields.io/badge/MC-1.14.4-FF69B4?style=flat-square) ![Forge 28.2.0](https://img.shields.io/badge/Forge-28.2.0-purple?style=flat-square) ![GitHub](https://img.shields.io/github/license/IzzelAliz/Arclight?style=flat-square)

![](.github/arclightlogo.jpg)

## Installing

1. Download the jar from [release page](https://github.com/IzzelAliz/Arclight/releases) or [build server](https://ci.appveyor.com/project/IzzelAliz/arclight/build/artifacts).
2. Launch with command `java -jar arclight-forge-mcversion-xxx.jar nogui`. The `nogui` argument will disable the server control panel.

## Support

Discord server https://discord.gg/ZvTY5SC

QQ Group chat 3556966

## Contributing

This project uses Gradle 4.9 as build tool with [arclight-gradle-plugin](https://github.com/IzzelAliz/arclight-gradle-plugin).

To setup development workspace, clone this repository first, and type
```
./gradlew remapSpigotJar idea
```

This will generate proper spigot sources and srg mappings.

Finally, import the project. IntelliJ IDEA is the recommended IDE.

Due to a [MixinGradle bug](https://github.com/SpongePowered/MixinGradle/issues/9), you may build the project twice or the mixin shadows won't get reobfuscated.

## License

This project in licensed under [GPL v3](LICENSE).
