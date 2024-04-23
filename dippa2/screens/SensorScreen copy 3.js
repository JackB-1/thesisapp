import React, { useState, useEffect } from 'react';
import { View, Text, Button, PermissionsAndroid } from 'react-native';
import { NativeModules } from 'react-native';
import useSensorData from '../components/SensorDataEmitter'; // Adjust the path as necessary


const SensorScreen = () => {
  const [imuData, setImuData] = useState([]);
  const [ecgData, setEcgData] = useState([]);
  const [genericData, setGenericData] = useState([]);
  const [mockData, setMockData] = useState([]);

  const { ReactNativeBridge } = NativeModules;
  

  const requestPermissions = async () => {
    await PermissionsAndroid.requestMultiple([
      PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN,
      PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
      PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
      PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS,
    ]);
  };

  useEffect(() => {
    requestPermissions();
  }, []);



  useSensorData((newMockData) => {
    console.log('New Mock Data received in SensorScreen:', newMockData);
    setMockData((currentData) => [...currentData, newMockData]);
  });

/*   // Use the custom hook to listen for both IMU and ECG data
  useSensorData(
    (genericData) => {
      console.log('New Generic Data received in SensorScreen:', genericData);
      setGenericData((currentData) => [...currentData, genericData]);
    },
    (newImuData) => {
      console.log('New IMU Data received in SensorScreen:', newImuData);
      setImuData((currentData) => [...currentData, newImuData]);
    },
    (newEcgData) => {
      console.log('New ECG Data received in SensorScreen:', newEcgData);
      setEcgData((currentData) => [...currentData, newEcgData]);
    }
  ); */

  // Function to start MoveSenseLog Activity
  const openMoveSenseLog = () => {
    ReactNativeBridge.startMoveSenseLog();
  };

  return (
    <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>

      <Button
        title="Open MoveSenseLog"
        onPress={openMoveSenseLog}
        color="#841584"
      />
      <Text>Mock Data:</Text>
      {mockData.map((data, index) => (
        <Text key={`mock-${index}`}>{data}</Text>
      ))}

      <Text>Generic Data:</Text>
      {genericData.map((data, index) => (
        <Text key={`generic-${index}`}>{data}</Text>
      ))}
      <Text>IMU Data:</Text>
      {imuData.map((data, index) => (
        <Text key={`imu-${index}`}>{data}</Text>
      ))}
      <Text>ECG Data:</Text>
      {ecgData.map((data, index) => (
        <Text key={`ecg-${index}`}>{data}</Text>
      ))}
    </View>
  );
};

export default SensorScreen;