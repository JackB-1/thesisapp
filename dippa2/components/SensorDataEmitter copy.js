import { useEffect } from 'react';
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

    useEffect(() => {
        console.log('useSensorData: Setting up');
        if (ReactNativeBridge) {
            const eventEmitter = new NativeEventEmitter(ReactNativeBridge);

            const mockDataListener = eventEmitter.addListener('MockDataEvent', (mockData) => {
                console.log('MockDataEvent received in useSensorData:', mockData, typeof mockData);
                let mockDataString;
                if (typeof mockData === 'string') {
                    mockDataString = mockData;
                } else {
                    mockDataString = String(mockData);
                }
                console.log('After conversion:', mockDataString, typeof mockDataString);
                if (onNewData) {
                    onNewData(mockDataString);
                }
            });

            return () => {
                console.log('useSensorData: Removing listener');
                mockDataListener.remove();
            };
        } else {
            console.log('useSensorData: ReactNativeBridge is not available');
        }
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