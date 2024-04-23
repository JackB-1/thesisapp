import AsyncStorage from '@react-native-async-storage/async-storage';

// Save sensor data
export const saveSensorData = async (sensorType, data) => {
  try {
    // Retrieve existing data for the sensor type
    const existingData = await AsyncStorage.getItem(sensorType);
    const newData = existingData ? JSON.parse(existingData) : [];
    
    // Add new data
    newData.push(data);
    
    // Save updated data back to storage
    await AsyncStorage.setItem(sensorType, JSON.stringify(newData));
    console.log(`Data saved for ${sensorType}`);
  } catch (error) {
    console.error('Error saving sensor data:', error);
  }
};

// Retrieve sensor data
export const getSensorData = async (sensorType) => {
  try {
    const data = await AsyncStorage.getItem(sensorType);
    return data ? JSON.parse(data) : [];
  } catch (error) {
    console.error('Error retrieving sensor data:', error);
    return [];
  }
};