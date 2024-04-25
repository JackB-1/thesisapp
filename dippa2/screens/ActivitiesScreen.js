import React, { useState } from 'react';
import { View, Text, Dimensions } from 'react-native';
import { Agenda } from 'react-native-calendars';
import { loadDataForDateRange } from '../components/DataStorage';
import { BarChart } from 'react-native-chart-kit';

const ActivitiesScreen = () => {
  const [items, setItems] = useState({});
  const [selectedDay, setSelectedDay] = useState('2024-04-16');
  const [dayData, setDayData] = useState([]);

  const colorMapping = {
    0: '#B34D4D', 1: '#FFB399', 2: '#FF33FF', 3: '#FFFF99', 4: '#00B3E6', 
    5: '#E6B333', 6: '#3366E6', 7: '#999966', 8: '#99FF99', 9: '#FF6633',
    10: '#80B300', 11: '#809900', 12: '#E6B3B3'
  };

  const loadItems = async (day) => {
    const startDate = new Date(day.year, day.month - 1, 1).getTime();
    const endDate = new Date(day.year, day.month, 0).getTime();
    const data = await loadDataForDateRange(startDate, endDate);

    const newItems = {};
    data.forEach((item) => {
      const strTime = new Date(item.timestamp).toISOString().split('T')[0];
      if (!newItems[strTime]) {
        newItems[strTime] = [];
      }
      newItems[strTime].push({
        name: new Date(item.timestamp).toLocaleTimeString('fi-FI', {
          hour: '2-digit', minute: '2-digit', second: '2-digit'
        }),
        modelOutput: item.modelOutput
      });
    });

    setItems(newItems);
  };

  const chartConfig = {
    backgroundGradientFrom: "#fff",
    backgroundGradientTo: "#fff",
    color: (opacity = 1) => `rgba(26, 255, 146, ${opacity})`,
    labelColor: (opacity = 1) => `rgba(0, 0, 0, ${opacity})`,
    barPercentage: 0.5,
  };

  const renderChart = () => {
    if (dayData.length === 0) {
      return <Text>No data for this day.</Text>;
    }

    const chartData = {
      labels: dayData.map(item => item.name),
      datasets: [{
        data: dayData.map(item => item.modelOutput),
        colors: dayData.map(item => (opacity = 1) => colorMapping[parseInt(item.modelOutput)])
      }]
    };

    return (
      <BarChart
        data={chartData}
        width={Dimensions.get('window').width - 20}
        height={220}
        yAxisLabel=""
        chartConfig={chartConfig}
        verticalLabelRotation={30}
        fromZero
      />
    );
  };

  return (
    <View style={{ flex: 1 }}>
      <Agenda
        items={items}
        loadItemsForMonth={loadItems}
        selected={selectedDay}
        onDayPress={(day) => {
          setSelectedDay(day.dateString);
          setDayData(items[day.dateString] || []);
        }}
        renderItem={({ item }) => <View />}
      />
      {renderChart()}
    </View>
  );
};

export default ActivitiesScreen;