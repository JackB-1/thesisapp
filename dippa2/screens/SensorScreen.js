import React, { useState, useCallback, useEffect } from 'react';
import { View, Text, ScrollView, Button, PermissionsAndroid, StyleSheet } from 'react-native';
import { NativeModules } from 'react-native';
import useSensorData from '../components/SensorDataEmitter'; // Adjust the path as necessary
import mapNumberToLabel from '../components/IMULabelMapper';

const SensorScreen = () => {
  const [imuData, setImuData] = useState([]);
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
    if (type === 'IMUDataEvent') {
      const label = mapNumberToLabel(data);
      setImuData((currentData) => {
        const newData = [...currentData, label];
        return newData.length > 5 ? newData.slice(1) : newData;
      });
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
    <View style={styles.container}>
      <Text style={styles.topHeader}>Sensors connected</Text>
      <ScrollView contentContainerStyle={styles.scrollView}>
        {sensorList.length > 0 ? (
          sensorList.map((name, index) => (
            <Text key={`sensor-${index}`} style={styles.sensorText}>{name}</Text>
          ))
        ) : (
          <Text style={styles.sensorText}>None</Text>
        )}
      </ScrollView>
      <Text style={styles.header}>Activity data</Text>
      <ScrollView contentContainerStyle={styles.scrollView}>
      {imuData.map((data, index) => (
        <Text key={`imu-${imuData.length - index - 1}`} style={{ ...styles.dataText, fontSize: 12 + index * 3 }}>{data}</Text>
      ))}
      </ScrollView>
      <Button
        title="Connect to sensors"
        onPress={openMoveSenseLog}
        color="#006fee"
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
    backgroundColor: '#f0f0f0'
  },
  topHeader: {
    fontSize: 18,
    fontWeight: 'bold',
    marginVertical: 10,
    paddingTop: 20
  },
  header: {
    fontSize: 18,
    fontWeight: 'bold',
    marginVertical: 10
  },
  topScrollView: {
    width: '100%',
    height: '40%',
    alignItems: 'center',
    marginBottom: 20
  },
  scrollView: {
    width: '100%',
    height: '60%',
    alignItems: 'center',
    marginBottom: 20
  },
  sensorText: {
    fontSize: 16,
    color: '#333'
  },
  dataText: {
    marginVertical: 2,
    color: '#555'
  }
});

export default SensorScreen;