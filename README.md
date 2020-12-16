# Availability

## MQTT Prerequirements
* Install [Java JDK 8 or above](https://www.oracle.com/java/technologies/javase/javase-jdk8-downloads.html)
* Install [Maven 3.5 or above](https://maven.apache.org/download.cgi)
* Install MQTT Broker (e.g. Mosquitto) and run it locally on port 1883
   * For a helpful tutorial refer to [Steve's Internet Guide](http://www.steves-internet-guide.com/install-mosquitto-broker/)

## Installation Guide
1. Clone repository to your machine
2. Open a terminal window (e.g. Command Prompt) and move to the root folder of the repository. Enter command `mvn clean install` This will create a target folder.
3. To ensure that installation was successful, check target folder for filter.jar file.
4. Move to target folder and enter command `java -jar filter.jar`. This will enable the Availability component to start listening to the MQTT Broker.

## Running Guide
1. Open Command Prompt, 'Run as Administrator' and move to mosquitto folder.
2. Run mosquitto -c mosquitto.conf command.
3. Open terminal for Frontend and run `npm run serve`.
4. Open localhost in web browser.
5. Run Booking, Dentist and Availability component.
6. Fill in form on booking webpage and click submit.
7. View Availability terminal to see if request was accepted or rejected. 
8. Check bookings.json in Booking component to see if request was saved or not.


