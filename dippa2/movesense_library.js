import { BleManager } from 'react-native-ble-plx';
import { Buffer } from 'buffer';
import { saveSensorData } from './dataStorage'; // Adjust the path as necessary

const WRITE_CHARACTERISTIC = "34800001-7185-4d5d-b431-630e7050e8f0";

const manager = new BleManager();

const connectToSensor = async (deviceId) => {
  try {
    console.log(`Attempting to connect to device with ID: ${deviceId}`); // Log attempt to connect
    const device = await manager.connectToDevice(deviceId);
    const home = await device.discoverAllServicesAndCharacteristics();
    console.log(`Connected to ${deviceId}`);
    console.log(home);
    return device;
  } catch (error) {
    console.error(`Connection error with device ${deviceId}:`, error);
  }
};

const writeToSensor = async (device, data) => {
  try {
    console.log(`Writing to sensor...`);
    await device.writeCharacteristicWithoutResponseForService(
      // Assuming the service UUID is the same as for subscribing
      '34802252-7185-4d5d-b431-630e7050e8f0',
      WRITE_CHARACTERISTIC,
      data // Data needs to be a base64 encoded string
    );
    console.log(`Write successful`);
  } catch (error) {
    console.error(`Write error:`, error);
  }
};

const subscribeToSensorData = async (device, sensorType, onDataReceived) => {
  const serviceUUID = '34802252-7185-4d5d-b431-630e7050e8f0'; // Updated service UUID
  const characteristicUUID = getCharacteristicUUID(sensorType); // Implement this function based on sensorType

  console.log(`Subscribing to ${sensorType} data...`); // Log subscription attempt
  await device.monitorCharacteristicForService(serviceUUID, characteristicUUID, (error, characteristic) => {
    if (error) {
      console.error(`Subscription error for ${sensorType}:`, error);
      return;
    }

    // Log the raw characteristic value directly to see if any data is coming through
    console.log(`Raw ${sensorType} data:`, characteristic.value);

    const data = parseSensorData(characteristic.value, sensorType);
    console.log(`Received ${sensorType} data:`, data); // Log received data
    onDataReceived(data);

    saveSensorData(sensorType, data);
  });
};

const parseSensorData = (rawData, sensorType) => {
  console.log(`Parsing ${sensorType} data...`); // Log parsing attempt
  const buffer = Buffer.from(rawData, 'base64');
  switch (sensorType) {
    case 'IMU6':
      return parseIMU6Data(buffer);
    case 'IMU9':
      return parseIMU9Data(buffer);
    // Implement other sensor types as needed
    default:
      console.error(`Unknown sensor type: ${sensorType}`);
      return null;
  }
};

const parseIMU6Data = (buffer) => {
  // Assuming IMU6 data format: [timestamp, accX, accY, accZ, gyroX, gyroY, gyroZ]
  // This is a simplified example. Adjust according to the actual data format.
  console.log('Parsing IMU6 data...', buffer, buffer.buffer);
  const dataView = new DataView(buffer.buffer);
  let offset = 0;
  const timestamp = dataView.getFloat32(offset, true); offset += 4;
  const accX = dataView.getFloat32(offset, true); offset += 4;
  const accY = dataView.getFloat32(offset, true); offset += 4;
  const accZ = dataView.getFloat32(offset, true); offset += 4;
  const gyroX = dataView.getFloat32(offset, true); offset += 4;
  const gyroY = dataView.getFloat32(offset, true); offset += 4;
  const gyroZ = dataView.getFloat32(offset, true); offset += 4;
  
  return { timestamp, accX, accY, accZ, gyroX, gyroY, gyroZ };
};

const parseIMU9Data = (buffer) => {
  // Assuming IMU9 data format includes magnetometer data in addition to IMU6
  // This is a simplified example. Adjust according to the actual data format.
  const imu6Data = parseIMU6Data(buffer);
  const dataView = new DataView(buffer.buffer, 28); // Starting after IMU6 data
  const magnX = dataView.getFloat32(0, true);
  const magnY = dataView.getFloat32(4, true);
  const magnZ = dataView.getFloat32(8, true);
  
  return { ...imu6Data, magnX, magnY, magnZ };
};

const disconnectFromSensor = async (device) => {
  console.log(`Disconnecting from device...`); // Log disconnect attempt
  await device.cancelConnection();
  console.log(`Disconnected from sensor`);
};

// Utility function to map sensor types to characteristic UUIDs
const getCharacteristicUUID = (sensorType) => {
  console.log(`Getting characteristic UUID for ${sensorType}`);
  // Updated mapping based on sensor specifications
  switch (sensorType) {
    case 'IMU6':
      return '34800002-7185-4d5d-b431-630e7050e8f0'; // Write characteristic UUID for IMU6
    case 'IMU9':
      return '34800002-7185-4d5d-b431-630e7050e8f0'; // Notify characteristic UUID for IMU9
    default:
      console.error(`Unknown sensor type: ${sensorType}`);
      return null;
  }
};

export { manager, connectToSensor, subscribeToSensorData, parseSensorData, disconnectFromSensor };