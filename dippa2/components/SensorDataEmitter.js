import { useEffect, useRef } from 'react';
import { NativeEventEmitter, NativeModules } from 'react-native';

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

const useSensorData = (onNewData) => {
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
            onNewData(imuData, 'IMUDataEvent');
        });

        const genericDataListener = eventEmitter.addListener('GenericEvent', (data) => {
            console.log('GenericEvent received in useSensorData:', data);
            onNewData(data, 'GenericEvent');
        });

        return () => {
            console.log('useSensorData: Removing listener');
            mockDataListener.remove();
            imuDataListener.remove();
            genericDataListener.remove();
        };
    }, [onNewData]);
};

export default useSensorData;


/* const useSensorData = (onNewData) => {
    useEffect(() => {
        console.log('useIMUData useEffect called');
        if (ReactNativeBridge) {
            console.log('ReactNativeBridge is available');
            console.log('ReactNativeBridge methods:', Object.keys(ReactNativeBridge));
            const eventEmitter = new NativeEventEmitter(ReactNativeBridge);
            console.log('NativeEventEmitter created');
            const imuDataListener = eventEmitter.addListener('IMUDataEvent', (imuData) => {
                console.log('IMUDataEvent received:', imuData);
                if (onNewData) {
                    onNewData(imuData);
                }
            });

            const genericDataListener = eventEmitter.addListener('GenericEvent', (genericData) => {
                console.log('GenericEvent received:', genericData);
                if (onNewData) {
                    onNewData(genericData);
                }
            });

            return () => {
                console.log('Removing IMUDataEvent listener');
                imuDataListener.remove();
                console.log('Removing GenericEvent listener'); // Add this line
                genericDataListener.remove(); // And this line
            };
        } else {
            console.log('ReactNativeBridge is not available');
        }
    }, [onNewData]);
}; 

export default useSensorData;

*/