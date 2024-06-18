import AsyncStorage from '@react-native-async-storage/async-storage';

// Function to save data incrementally
export const saveData = async (data) => {
    const key = `data_${data.timestamp}`;
    try {
        await AsyncStorage.setItem(key, JSON.stringify(data));
        console.log('Data saved:', data);
    } catch (error) {
        console.error('Failed to save data', error);
    }
};

// Function to load data for a specific date range
export const loadDataForDateRange = async (startDate, endDate) => {
    try {
        const keys = await AsyncStorage.getAllKeys();
        const dateKeys = keys.filter(key => {
            const timestamp = parseInt(key.split('_')[1], 10);
            return timestamp >= startDate && timestamp <= endDate;
        });
        const stores = await AsyncStorage.multiGet(dateKeys);
        const data = stores.map(([key, value]) => JSON.parse(value));
        console.log('Data loaded for date range:', data);
        return data;
    } catch (error) {
        console.error('Failed to load data for date range', error);
        return [];
    }
};

// Optional: Function to load all data (not recommended for large datasets)
export const loadAllData = async () => {
    try {
        const keys = await AsyncStorage.getAllKeys();
        const stores = await AsyncStorage.multiGet(keys);
        const data = stores.map(([key, value]) => JSON.parse(value));
        console.log('All data loaded:', data);
        return data;
    } catch (error) {
        console.error('Failed to load all data', error);
        return [];
    }
};