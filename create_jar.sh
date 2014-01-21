cd ~/projects/TimeShifter
javac -cp ./lib/javassist.jar:./lib/slf4j-api-1.7.5.jar:./lib/slf4j-simple-1.7.5.jar -d classes src/timeshifter/*
cd classes
touch MANIFEST.MF
echo "Manifest-Version: 1.0" > MANIFEST.MF
echo "Premain-Class: timeshifter.MainClass" >> MANIFEST.MF
jar -cvmf MANIFEST.MF timeshifter.jar ./timeshifter/*
