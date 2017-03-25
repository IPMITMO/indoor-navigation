from fabric.api import *

env.hosts = [
		"192.168.43.123",
		"192.168.43.94",
		"192.168.43.175",
]

env.user = "pi"
env.password = "raspberry"

def uname():
	run("uname")

def deploy():
	put("beacon_control/start_beacon.py", "~/posprj")
	put("beacon_control/stop_beacon.py", "~/posprj")

def start_beacons():
	run("sudo python3 ~/posprj/start_beacon.py")

def stop_beacons():
	run("sudo python3 ~/posprj/stop_beacon.py")
