import React, { useState } from 'react';
import { View, Text } from 'react-native'; // Import View and Text from react-native
import { Agenda } from 'react-native-calendars';

const ActivitiesScreen = () => {
  const [items, setItems] = useState({});

  const loadItems = (day) => {
    setTimeout(() => {
      const newItems = { ...items }; // Create a shallow copy of items to modify
      for (let i = -15; i < 85; i++) {
        const time = day.timestamp + i * 24 * 60 * 60 * 1000;
        const strTime = new Date(time).toISOString().split('T')[0];
        if (!newItems[strTime]) {
          newItems[strTime] = [];
          const numItems = Math.floor(Math.random() * 3 + 1);
          for (let j = 0; j < numItems; j++) {
            newItems[strTime].push({
              name: 'Item for ' + strTime + ' #' + j,
              height: Math.max(50, Math.floor(Math.random() * 150)),
            });
          }
        }
      }
      setItems(newItems); // Update the state with the modified copy
    }, 1000);
  };

  const renderItem = (item) => {
    return (
      <View style={{ marginRight: 10, marginTop: 17 }}>
        <Text>{item.name}</Text>
      </View>
    );
  };

  return (
    <Agenda
      items={items}
      loadItemsForMonth={loadItems}
      selected={'2023-05-16'}
      renderItem={renderItem}
    />
  );
};

export default ActivitiesScreen;