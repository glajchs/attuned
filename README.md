## One size fits all install script
```
sudo apt-get install git maven openjdk-8-jdk libgstreamer1.0-0 libgstreamer-plugins-base1.0-0 libx11-dev
git clone https://github.com/glajchs/attuned.git
cd attuned
mvn clean install
java -cp target/Attuned-0.0.2-SNAPSHOT-jar-with-dependencies.jar com.attuned.main.AttunedMainWindow &
```
#### Currently only linux64 bit build supported

### Current required deb packages to build the project
git
maven
openjdk-8-jdk (any JDK works, but the JRE is the only thing installed on base systems, not the JDK, openjdk-7-jdk works for older systems)

### Current required deb packages to run the jar

### mp3 playback
libgstreamer1.0
libgstreamer-plugins-base1.0-0

### keybinds
libx11-dev  

## About Attuned
A small music program I wrote with the purpose of being quick to use in the background.

I was frustrated that no OS seems to have a good music player anymore with what Winamp used to have for global keyboard shortcuts.

## Key features

- Global keybinds for most audio functions and a "jump to song" search box bound via global keybind.
- Change your song while playing your game/coding/doing anything else without requiring an alt+tab.
- Jump to song via keybind (search box appears) while playing your game/coding/doing anything else without requiring an alt+tab.
- Has a simple (and better) shuffle support than most players.  Will play whole library in artist/album/track order, and supports shuffling,
    but when you toggle shuffle off, it will resume in library order (i.e. next song in current album).
- I plan to make this cross-OS (thus being based on Java and SWT), but I haven't added the OS-specific bits yet (key bindings) to anything other than linux
- Will support (doesn't yet) saving custom song position markers, and will allow you to use a hotkey to jump to those markers (great for long mixes).
- Requires a lot of tweaking to be made good for general use, and is Linux only thusfar.
