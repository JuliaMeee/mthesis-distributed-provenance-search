# Distributed provenance system

This is an implementation of integrity and non-repudiation into a system of distributed provenance. The full text of the thesis to which this implementation belongs it available at https://is.muni.cz/th/mo8f1/.

This repository consists of two web applications -- **Trusted party** and **Provenance storage**.

## How to run the applications

In directories **./trusted\_party** and **./distributed\_prov\_system** there are scripts **manage.py**. For local
testing and development purposes this script can run a webserver by running `python manage.py runserver 127.0.0.1 8000`. 
This runs the service on localhost accessible under a TCP port 8000.

Both of the applications can be run this way, however mind that a working connection to a database
is required which is set in **settings.py** of individual applications.

Note that the `runserver` command should not be used in production. For more information about
deploying an application into production see [How to deploy Django](https://docs.djangoproject.com/en/5.0/howto/deployment/).

## Demo simulation

As part of the thesis a demo simulation was performed which is described and accessible under link https://is.muni.cz/th/mo8f1/.
As part of the thesis an archive with curls and used PROV documents were published which can be used for testing purposes
whether the applications are deployed correctly. For more information please refer to the text part of the thesis.