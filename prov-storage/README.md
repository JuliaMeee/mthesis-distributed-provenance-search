# Distributed provenance system

This is an implementation of integrity and non-repudiation into a 
system of distributed provenance. The full text of the thesis to which 
this implementation belongs it available at https://is.muni.cz/th/mo8f1/.

The implementation was tested and changed for another thesis, available at https://is.muni.cz/th/du7ec/.
This readme contains original guide how to run the application and also new guide for running the tests.

This repository consists of two web applications -- **Trusted party** 
and **Provenance storage**.

## How to run the applications

In directories **./trusted\_party** and **./distributed\_prov\_system** there are scripts **manage.py**. For local
testing and development purposes this script can run a webserver by running `python manage.py runserver 127.0.0.1 8000`. 
This runs the service on localhost accessible under a TCP port 8000.

Both of the applications can be run this way, however mind that a working connection to a database
is required which is set in **settings.py** of individual applications.

Note that the `runserver` command should not be used in production. For more information about
deploying an application into production see [How to deploy Django](https://docs.djangoproject.com/en/5.0/howto/deployment/).

## Update - tests of the provenance system

In the current thesis (https://is.muni.cz/th/du7ec/) the tests for the application were implemented.
They can be found in folders `distributed-provenance-system\distributed_prov_system_tests` and
`distributed-provenance-system\trusted_party_tests`.

All tests can be launched from IDE if properly set up. 

To launch tests from command line from the `distributed-provenance-system` folder execute these commands:

`virtualenv venv`

`venv\Scripts\activate` on windows 
`source venv/bin/activate` on linux

To launch tests, all required libraries must be installed.
`pip install -r requirements.txt`

Following directories should be marked as source root folders for launching the tests from IDE:

`provenance-system-tests\distributed-provenance-system\distributed_prov_system`

`provenance-system-tests\distributed-provenance-system\distributed_prov_system_tests`

`provenance-system-tests\distributed-provenance-system\trusted_party`

to add them to PYTHONPATH use these commands on Linux:

`export PYTHONPATH="${PYTHONPATH}:/provenance-system-tests/distributed-provenance-system/distributed_prov_system"`

`export PYTHONPATH="${PYTHONPATH}:/provenance-system-tests/distributed-provenance-system/distributed_prov_system_tests"`

`export PYTHONPATH="${PYTHONPATH}:/provenance-system-tests/distributed-provenance-system/trusted_party"`

Or these in Powershell on Windows:

`$env:PYTHONPATH = "${env:PYTHONPATH};C:\skola\diplomka\test_result\distributed-provenance-system\trusted_party"`

`$env:PYTHONPATH = "${env:PYTHONPATH};C:\skola\diplomka\test_result\distributed-provenance-system\distributed_prov_system"`

`$env:PYTHONPATH = "${env:PYTHONPATH};C:\skola\diplomka\test_result\distributed-provenance-system\distributed_prov_system_tests"`



To launch tests, go to folder that contains them and use `pytest FILE_NAME`

For component tests, use these commands when in the distributed-provenance-system folder to build and launch the application:

This is also needed for trusted party unit test now, if it is not wanted, uncomment code for launching the database in models and controller unit tests of trusted party.

`docker build --no-cache -f Dockerfile.ProvStorage -t distributed_prov_system .`

`docker build --no-cache -f Dockerfile.TrustedParty -t trusted_party .`

`docker compose up -d`

Then just launch the tests, same way as for unit tests.

## Update - changes in the original code

Changes were made to the original code and are marked by comments:

`# change` - changes done for current thesis https://is.muni.cz/th/du7ec/  

`# added` - changes done for Distributed Provenance Demo (https://gitlab.ics.muni.cz/422328/dbprov/)

`# fix` - fixes done for current thesis https://is.muni.cz/th/du7ec/ 

