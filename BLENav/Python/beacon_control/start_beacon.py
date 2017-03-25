from bluetooth.ble import BeaconService

service = BeaconService()

service.start_advertising("11111111-2222-3333-4444-555555555555",
		1, 1, 1, 200)
