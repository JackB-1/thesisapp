import React, { useState, useCallback, useEffect } from 'react';
import { View, Text, ScrollView, Button, PermissionsAndroid } from 'react-native';
import { NativeModules } from 'react-native';
import useSensorData from '../components/SensorDataEmitter'; // Adjust the path as necessary
import mapNumberToLabel from '../components/IMULabelMapper';

const SensorScreen = () => {
  const [mockData, setMockData] = useState([]);
  const [imuData, setImuData] = useState([]);
  const [genericData, setGenericData] = useState([]);
  const [sensorList, setSensorList] = useState([]);

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

  const handleNewData = useCallback((data, type) => {
    if (type === 'MockDataEvent') {
      setMockData((currentData) => [...currentData, data]);
    } else if (type === 'IMUDataEvent') {
      const label = mapNumberToLabel(data);
      setImuData((currentData) => [...currentData, label]);
    } else if (type === 'GenericEvent') {
      setGenericData((currentData) => [...currentData, data]);
    } else {
      console.log('SensorScreen: Unknown event type:', type);
    }
  }, []);

  const handleSensorsUpdated = useCallback((sensorNames) => { 
    const parsedSensorNames = JSON.parse(sensorNames).sensorNames
    console.log("parsedSensorNames:", parsedSensorNames)
    setSensorList(parsedSensorNames);
  }, []);

  useSensorData(handleNewData, handleSensorsUpdated);

  const openMoveSenseLog = () => {
    ReactNativeBridge.startMoveSenseLog();
  };

  return (
    <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
      <Text>Sensors List:</Text>
      <ScrollView contentContainerStyle={{ alignItems: 'center' }} style={{ width: '100%', height: '20%' }}>
        {sensorList.map((name, index) => (
          <Text key={`sensor-${index}`}>{name}</Text>
        ))}
      </ScrollView>
      <Text>Mock Data:</Text>
      <ScrollView style={{ width: '100%', height: '20%' }}>
        {mockData.map((data, index) => (
          <Text key={`mock-${index}`}>{data}</Text>
        ))}
      </ScrollView>
      <Text>IMU Data:</Text>
      <ScrollView contentContainerStyle={{ alignItems: 'center' }} style={{ width: '100%', height: '20%' }}>
        {imuData.map((data, index) => (
          <Text key={`imu-${index}`}>{data}</Text>
        ))}
      </ScrollView>
      <Text>Generic Data:</Text>
      <ScrollView style={{ width: '100%', height: '20%' }}>
        {genericData.map((data, index) => (
          <Text key={`generic-${index}`}>{data}</Text>
        ))}
      </ScrollView>
      <Button
        title="Open MoveSenseLog"
        onPress={openMoveSenseLog}
        color="#006fee"
      />
    </View>
  );
};

export default SensorScreen;