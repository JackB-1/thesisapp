import React from 'react';
import { BottomNavigation } from 'react-native-paper';
import { Icon } from 'react-native-elements';
import SensorScreen from './screens/SensorScreen';
import ActivitiesScreen from './screens/ActivitiesScreen';
import NotificationsScreen from './screens/NotificationsScreen';

const SensorRoute = () => <SensorScreen />;
const ActivitiesRoute = () => <ActivitiesScreen />;
const NotificationsRoute = () => <NotificationsScreen />;

const AppNavigator = () => {
  const [index, setIndex] = React.useState(0);
  const [routes] = React.useState([
    { key: 'sensor', title: 'Sensors', icon: 'sensors', color: '#6200ee' },
    { key: 'activities', title: 'Activities', icon: 'event', color: '#007bff' },
    { key: 'tools', title: 'Tools', icon: 'tools', color: '#f44336' },
  ]);

  const renderIcon = ({ route, focused, color }) => {
    let iconName = '';
    let iconType = '';
    if (route.key === 'sensor') {
      iconName = 'sensors';
      iconType = 'material'; 
    } else if (route.key === 'activities') {
      iconName = 'event';
      iconType = 'material';
    } else if (route.key === 'tools') {
      iconName = 'settings';
      iconType = 'material';
    }

    return <Icon name={iconName} type={iconType} size={24} color={color} />;
  };

  const renderScene = BottomNavigation.SceneMap({
    sensor: SensorRoute,
    activities: ActivitiesRoute,
    tools: NotificationsRoute,
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