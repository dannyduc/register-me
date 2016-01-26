#RegisterMe

To build:

    mvn package assembly:single

To run:

    java -jar target/register-me-jar-with-dependencies.jar \
        username/password ...
        
Cronjob:
    
    cp target/register-me-jar-with-dependencies.jar ~/bin
         
    crontab -l
    
    0 0 * * * ~/bin/register
    
    crontab -e