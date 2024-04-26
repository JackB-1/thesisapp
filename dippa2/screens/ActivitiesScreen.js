import React, { useState, useEffect } from 'react';
import { View, Text, Dimensions } from 'react-native';
import { Calendar } from 'react-native-calendars';
import { loadDataForDateRange } from '../components/DataStorage';
import { VictoryBar, VictoryChart, VictoryTheme, VictoryLegend, VictoryAxis } from 'victory-native';

const ActivitiesScreen = () => {
  const [selectedDay, setSelectedDay] = useState('2024-04-16');
  const [dayData, setDayData] = useState([]);
  const [isLoading, setIsLoading] = useState(false);

  const colorMapping = {
    0: '#B34D4D', 1: '#FFB399', 2: '#FF33FF', 3: '#FFFF99', 4: '#00B3E6',
    5: '#E6B333', 6: '#3366E6', 7: '#999966', 8: '#99FF99', 9: '#FF6633',
    10: '#80B300', 11: '#809900', 12: '#E6B3B3'
  };

  const legendData = Object.keys(colorMapping).map(key => ({
    name: `${key}`,
    symbol: { fill: colorMapping[key] }
  }));

  const loadItems = async (date) => {
    setIsLoading(true);
    const dayStart = new Date(date);
    const dayEnd = new Date(dayStart);
    dayEnd.setDate(dayStart.getDate() + 1); // Set end date to the next day

    const startDate = dayStart.getTime();
    const endDate = dayEnd.getTime();

    const data = await loadDataForDateRange(startDate, endDate);
    const formattedData = data.map(item => ({
      name: new Date(item.timestamp).toLocaleTimeString('fi-FI', {
        hour: '2-digit', minute: '2-digit', second: '2-digit'
      }),
      modelOutput: item.modelOutput
    }));
    setDayData(formattedData);
    setIsLoading(false);
  };

  useEffect(() => {
    loadItems(selectedDay);
  }, [selectedDay]);

  const renderChart = () => {
    if (isLoading) {
      return <Text style={{ fontSize: 20, textAlign: 'center', flex: 1, justifyContent: 'center', alignItems: 'center', paddingTop: 10 }}>Data is being fetched...</Text>;
    }
    if (dayData.length === 0) {
      return <Text style={{ fontSize: 20, textAlign: 'center', flex: 1, justifyContent: 'center', alignItems: 'center', paddingTop: 10  }}>No data for this day.</Text>;
    }

    // Calculate tick values to show only 10 labels or fewer based on available data
    const tickInterval = Math.ceil(dayData.length / 10);
    const tickValues = dayData
      .map((item, index) => ({ index, name: item.name }))
      .filter((item, index) => index % tickInterval === 0)
      .map(item => item.name);
    console.log(tickValues);

    return (
      <VictoryChart
        width={Dimensions.get('window').width}
        theme={VictoryTheme.material}
        domainPadding={20} // Adjust domain padding to ensure bars are not cut off
        domain={{ x: [0, dayData.length - 1] }}
      >
        <VictoryLegend x={0} y={0}
          title="Activities"
          centerTitle
          orientation="horizontal"
          gutter={10}
          style={{ border: { stroke: "black" }, title: { fontSize: 14 } }}
          data={legendData}
          symbolSpacer={5}
        />
        <VictoryBar
          dependentAxis={true}
          data={dayData}
          x="name"
          y="modelOutput"
          style={{
            data: {
              width: 1,
              stroke: ({ datum }) => colorMapping[datum.modelOutput],
              strokeWidth: 1,
              padding: 1,
            }
          }}
          alignment="middle" // Ensure bars are aligned in the middle of their data points
        />
        <VictoryAxis 
        dependentAxis
        tickCount={12}
        tickInterval={1}
        tickValues={[0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10]}
        />
        <VictoryAxis
          tickValues={[0, 1, 2, 3, 4, 5, 6, 7, 8, 9]}
          tickFormat={(t) => t}
          tickCount={10}
          style={{ tickLabels: { angle: -45,  fontSize: 12, padding: 14 } }}
          crossAxis
          width={400}
          label="Time"
          labelStyle={{ fontSize: 24 }}
          offsetX={25}
        />
        {/* <VictoryAxis
          dependentAxis
          tickValues={tickValues}
          tickFormat={(t) => t} // Directly use the tick value as the label
          tickCount={10}
          style={{ tickLabels: { angle: -45,  fontSize: 8 } }}
          axisValue={tickValues}
          width={400}
        /> */}
      </VictoryChart>
    );
  };

  return (
    <View style={{ flex: 1 }}>
      <Calendar
        onDayPress={(day) => {
          setSelectedDay(day.dateString);
        }}
        markedDates={{
          [selectedDay]: { selected: true, marked: true }
        }}
      />
      {renderChart()}
    </View>
  );
};

export default ActivitiesScreen;