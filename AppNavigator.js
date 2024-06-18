import React from 'react';
import { BottomNavigation } from 'react-native-paper';
import { Icon } from '@rneui/themed'; // Updated import
import SensorScreen from './screens/SensorScreen';
import ActivitiesScreen from './screens/ActivitiesScreen';

const SensorRoute = () => <SensorScreen />;
const ActivitiesRoute = () => <ActivitiesScreen />;

const AppNavigator = () => {
  const [index, setIndex] = React.useState(0);
  const [routes] = React.useState([
    { key: 'sensor', title: 'Sensors', icon: 'sensors', color: '#6200ee' },
    { key: 'activities', title: 'Activities', icon: 'event', color: '#007bff' }
  ]);

  const renderIcon = ({ route, focused, color }) => {
    let iconName = '';
    let iconType = 'material'; // Default type
    let iconProps = {};

    switch (route.key) {
      case 'sensor':
        iconName = 'sensors';
        iconProps = { color: '#6200ee' }; // Example specific color
        break;
      case 'activities':
        iconName = 'event';
        iconProps = { color: '#007bff' }; // Example specific color
        break;
      default:
        iconName = 'error'; // Fallback icon
        iconType = 'material'; // Default type for fallback
        iconProps = { color: '#000' }; // Default color for fallback
    }
    // iconProps.raised = false; 

    return <Icon name={iconName} type={iconType} size={24} {...iconProps} />;
};

  const renderScene = BottomNavigation.SceneMap({
    sensor: SensorRoute,
    activities: ActivitiesRoute,
  });

  return (
    <BottomNavigation
      navigationState={{ index, routes }}
      onIndexChange={setIndex}
      renderScene={renderScene}
      renderIcon={renderIcon}
      shifting={false}
      sceneAnimationEnabled={true}
    />
  );
};

export default AppNavigator;