### One size fits all install script ###
sudo apt-get install git maven openjdk-7-jdk libgstreamer0.10-dev libgstreamer-plugins-base0.10-dev libX11-dev
git clone git@github.com:glajchs/attuned.git
cd attuned
mvn clean install
java -cp target/Attuned-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.attuned.main.AttunedMainWindow &

### Currently only linux64 bit build supported ###

### Current required deb packages to build the project ###
git
maven
openjdk-7-jdk (any JDK works, but the JRE is the only thing installed on base systems, not the JDK)

### Current required deb packages to run the jar ###

# mp3 playback
libgstreamer0.10-dev
libgstreamer-plugins-base0.10-dev

# keybinds
libX11-dev