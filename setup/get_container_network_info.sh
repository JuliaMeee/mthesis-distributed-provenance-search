#!/bin/bash

CONTAINER_NAME=$1

INNER_IP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' $CONTAINER_NAME)

# Get the container's inner port and host port
PORTS=$(docker inspect -f '{{range $p, $conf := .NetworkSettings.Ports}}{{if $conf}}{{(index $conf 0).HostPort}}:{{(index (split $p "/") 0)}} {{end}}{{end}}' $CONTAINER_NAME)
# Assign the inner port and host port to variables
IFS=':' read -r HOST_PORT INNER_PORT <<< "$PORTS"

# Return the values
echo "$INNER_IP $INNER_PORT $HOST_PORT"