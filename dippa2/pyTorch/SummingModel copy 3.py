import torch
import torch.nn as nn


class SummingModel(nn.Module):
    def __init__(self, num_sensors=6, sensor_data_length=104):
        super(SummingModel, self).__init__()
        # Assuming each sensor data array has a fixed length (e.g., 60)
        self.num_sensors = num_sensors
        self.sensor_data_length = sensor_data_length

    def forward(self, input_tensor):
        # Assuming input_tensor is a 1D tensor containing all sensor data concatenated
        # Split the tensor into separate tensors for each sensor
        sensor_tensors = torch.split(input_tensor, self.sensor_data_length)
        
        max_sum = float('-inf')
        max_name = 'faulty_output'
        sensor_names = ['acc_x', 'acc_y', 'acc_z', 'gyro_x', 'gyro_y', 'gyro_z']
        
        for i, sensor_tensor in enumerate(sensor_tensors):
            current_sum = torch.sum(sensor_tensor).item()
            if current_sum > max_sum:
                max_sum = current_sum
                max_name = sensor_names[i]
        
        # Output the name of the sensor array with the largest sum
        return max_name

# Example usage
model = SummingModel()  # Adjust sensor_data_length as needed
# Example tensor input (flattened sensor data)
input_tensor = torch.randn(624)  # Example tensor with random data
output = model(input_tensor)
print(f"The sensor array with the largest sum is: {output}")



""" scripted_model = torch.jit.script(model) """
new_scripted_model = torch.jit.script(SummingModel())

print(model)
print(scripted_model)

""" scripted_model.save("summing_model.ptl") """
""" new_scripted_model.save("summing_model.ptl")
 """




""" # Save the model
torch.save(model, 'summing_model.pt')
 """