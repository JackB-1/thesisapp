import { useEffect, useRef } from 'react';
import { NativeEventEmitter, NativeModules } from 'react-native';
import { saveData } from './DataStorage';

const { ReactNativeBridge } = NativeModules;

const useMockDataEmitter = () => {
    useEffect(() => {
        console.log('useMockDataEmitter: Starting mock data emitter');
        ReactNativeBridge.startMockDataEmitter();

        return () => {
            console.log('useMockDataEmitter: Stopping mock data emitter');
            ReactNativeBridge.stopMockDataEmitter();
        };
    }, []);
};

const useSensorData = (onNewData, onSensorsUpdated) => {
    useMockDataEmitter(); // Ensure the mock data emitter starts when this hook is used

    const eventEmitterRef = useRef(new NativeEventEmitter(ReactNativeBridge));

    useEffect(() => {
        const eventEmitter = eventEmitterRef.current;

        const mockDataListener = eventEmitter.addListener('MockDataEvent', (mockData) => {
            console.log('MockDataEvent received in useSensorData:', mockData);
            onNewData(mockData, 'MockDataEvent');
        });

        const imuDataListener = eventEmitter.addListener('IMUDataEvent', (imuData) => {
            console.log('IMUDataEvent received in useSensorData:', imuData);
            const parsedData = JSON.parse(imuData);
            saveData(parsedData); // Save the parsed data to AsyncStorage
            onNewData(parsedData, 'IMUDataEvent');
        });

        const sensorsUpdatedListener = eventEmitter.addListener('sensorsUpdated', (sensorNames) => {
            console.log('sensorsUpdated event received:', sensorNames);
            onSensorsUpdated(sensorNames);
        });

        return () => {
            console.log('useSensorData: Removing listener');
            mockDataListener.remove();
            imuDataListener.remove();
            sensorsUpdatedListener.remove();
        };
    }, [onNewData, onSensorsUpdated]);
};

export default useSensorData;
