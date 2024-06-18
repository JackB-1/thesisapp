import { useEffect, useRef } from 'react';
import { NativeEventEmitter, NativeModules } from 'react-native';
import { saveData } from './DataStorage';

const { ReactNativeBridge } = NativeModules;


const useSensorData = (onNewData, onSensorsUpdated) => {
    const eventEmitterRef = useRef(new NativeEventEmitter(ReactNativeBridge));

    useEffect(() => {
        const eventEmitter = eventEmitterRef.current;

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
            imuDataListener.remove();
            sensorsUpdatedListener.remove();
        };
    }, [onNewData, onSensorsUpdated]);
};

export default useSensorData;
