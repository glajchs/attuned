## One size fits all install script
```
sudo apt-get install git maven openjdk-7-jdk libgstreamer0.10-dev libgstreamer-plugins-base0.10-dev libX11-dev
git clone https://github.com/glajchs/attuned.git
cd attuned
mvn clean install
java -cp target/Attuned-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.attuned.main.AttunedMainWindow &
```
### Currently only linux64 bit build supported

### Current required deb packages to build the project
git  
maven  
openjdk-7-jdk (any JDK works, but the JRE is the only thing installed on base systems, not the JDK)  

### Current required deb packages to run the jar

#### mp3 playback
libgstreamer0.10-dev  
libgstreamer-plugins-base0.10-dev  

#### keybinds
libX11-dev  


A small music program I wrote with the purpose of being quick to use in the background.
I was frustrated that no OS seems to have a good music player anymore with what Winamp used to have for global keyboard shortcuts.
Key features are global keybinds for most audio functions and a "jump to song" search box bound via global keybind.
Change your song while playing your game/coding/doing anything else without requiring an alt+tab.
I plan to make this cross-OS (thus being based on Java and SWT)
Requires a lot of tweaking to be made good for general use, and is Linux only thusfar.
