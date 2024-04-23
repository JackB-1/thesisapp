import React, { useState, useEffect } from 'react';
import { PermissionsAndroid, View, Text, FlatList, Button, TouchableOpacity } from 'react-native';
import { connectToSensor, subscribeToSensorData, manager } from '../movesense_library'; // Adjust the path as necessary

import { NativeModules } from 'react-native';
import useIMUData from '../components/SensorDataEmitter';

const SensorScreen = () => {
  const [devices, setDevices] = useState([]);
  const [movesenseDevices, setMovesenseDevices] = useState([]);
  const [isScanning, setIsScanning] = useState(false);
  const [connectedDevice, setConnectedDevice] = useState(null);
  const [sensorData, setSensorData] = useState([]);

  const { ReactNativeBridge } = NativeModules;

  const handleNewIMUData = (newData) => {
    setSensorData((prevData) => [...prevData, newData]);
  };

  const requestPermissions = async () => {
    await PermissionsAndroid.requestMultiple([
      PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN,
      PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
      PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
    ]);
  };

  useEffect(() => {
    requestPermissions();
  }, []);

  const startScan = () => {
    if (!isScanning) {
      setDevices([]);
      setMovesenseDevices([]);
      setIsScanning(true);
      manager.startDeviceScan(null, null, (error, device) => {
        if (error) {
          console.error(error);
          setIsScanning(false);
          return;
        }

        if (device) {
          handleDeviceFound(device);
        }
      });

      // Define a timeout to stop the scan after 10 seconds
      setTimeout(() => {
        console.log("Automatically stopping scan after 10 seconds");
        manager.stopDeviceScan();
        setIsScanning(false);
      }, 10000); // Stop scanning after 10 seconds
    }
  };

  const handleDeviceFound = (device) => {
    if (device.name && device.name.includes("Movesense")) {
      setMovesenseDevices((prevState) => {
        // Check if the device is already in the list
        if (prevState.find((d) => d.id === device.id)) {
          return prevState; // Return the current state if the device is already added
        }
        return [...prevState, device]; // Add the new device to the state
      });
    } else {
      setDevices((prevState) => {
        // Check if the device is already in the list
        if (prevState.find((d) => d.id === device.id)) {
          return prevState; // Return the current state if the device is already added
        }
        return [...prevState, device]; // Add the new device to the state
      });
    }
  };

  /* const handleDeviceFound = (device) => {
    if (device.name && device.name.includes("Movesense")) {
      setMovesenseDevices((prevState) => {
        const deviceIndex = prevState.findIndex((d) => d.id === device.id);
        if (deviceIndex === -1) {
          return [...prevState, device];
        } else {
          const updatedDevices = [...prevState];
          updatedDevices[deviceIndex] = device;
          return updatedDevices;
        }
      });
    } else {
      setDevices((prevState) => {
        const deviceIndex = prevState.findIndex((d) => d.id === device.id);
        if (deviceIndex === -1) {
          return [...prevState, device];
        } else {
          const updatedDevices = [...prevState];
          updatedDevices[deviceIndex] = device;
          return updatedDevices;
        }
      });
    }
  }; */

  const handleConnectToDevice = async (deviceId) => {
    const device = await connectToSensor(deviceId);
    if (device) {
      setConnectedDevice(device);
      // Example: Subscribe to IMU6 data. Adjust according to your needs.
      subscribeToSensorData(device, 'IMU6', (data) => {
        setSensorData((prevData) => [...prevData, data]);
      });
    }
  };

  const renderItem = ({ item }) => (
    <TouchableOpacity onPress={() => handleConnectToDevice(item.id)}>
      <Text>{item.name || 'Unnamed device'} - {item.id}</Text>
    </TouchableOpacity>
  );

  useIMUData(handleNewIMUData);

  // Function to start MoveSenseLog Activity
  const openMoveSenseLog = () => {
    ReactNativeBridge.startMoveSenseLog();
  };

  return (
    <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: '#d2d2d2' }}>
      {/* UI for scanning and listing devices */}
      {/* <Button
        title="Open MoveSenseLog"
        onPress={() => ReactNativeBridge.startMoveSenseLogActivity()}
      /> */}

      <Button
        title="Open MoveSenseLog"
        onPress={openMoveSenseLog}
        color="#841584"
      />

      <Button title="Scan for devices" onPress={startScan} disabled={isScanning} color="#841584" />
      <FlatList
        data={movesenseDevices}
        keyExtractor={(item) => item.id}
        renderItem={renderItem}
        style={{ width: '100%' }}
      />
      {/* Display connected device */}
      {connectedDevice && (
        <Text>Connected to: {connectedDevice.name}</Text>
      )}
      <Text>DATA: {sensorData}</Text>
      {/* Display sensor data */}
      <FlatList
        data={sensorData}
        keyExtractor={(_, index) => index.toString()}
        renderItem={({ item }) => <Text>{JSON.stringify(item)}</Text>}
        style={{ width: '100%' }}
      />
    </View>

  );
};

export default SensorScreen;