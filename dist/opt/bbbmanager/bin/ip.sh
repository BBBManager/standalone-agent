#!/bin/bash
/bin/ip addr show eth0 | grep inet\  | awk -F ' ' '{print $2}' | awk -F '/' '{print $1}'