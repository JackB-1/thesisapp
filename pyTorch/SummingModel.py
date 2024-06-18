import torch
import torch.nn as nn
from torch.utils.mobile_optimizer import optimize_for_mobile


class SummingModel(nn.Module):
    def __init__(self, num_sensors=6, sensor_data_length=104):
        super(SummingModel, self).__init__()
        self.num_sensors = num_sensors
        self.sensor_data_length = sensor_data_length

    def forward(self, input_tensor):
        # Ensure correct dimension is split
        sensor_tensors = torch.split(input_tensor, self.sensor_data_length, dim=1)
        max_sum = float('-inf')
        max_index = -1

        for i, sensor_tensor in enumerate(sensor_tensors):
            current_sum = torch.sum(sensor_tensor)
            if current_sum > max_sum:
                max_sum = current_sum
                max_index = i

        # Return the index of the sensor with the maximum sum as a tensor
        return torch.tensor([max_index], dtype=torch.long)


# Initialize the model and set it to evaluation mode
model = SummingModel()
model.eval()

# Assuming a 1D tensor for all concatenated sensor data as input
# Adjust the size of the example input tensor as needed
# Adjust the size as needed for your model
example_input_tensor = torch.randn(1, 624)
traced_script_module = torch.jit.script(model)

# Optimize the traced model for mobile
optimized_traced_model = optimize_for_mobile(traced_script_module)

print(torch.__version__)

# Save the optimized model for mobile
# optimized_traced_model._save_for_lite_interpreter("app/src/main/assets/summing_model.ptl")
optimized_traced_model._save_for_lite_interpreter(
    "C:/dev2/dippa2/android/app/src/main/assets/summingModel.ptl")
