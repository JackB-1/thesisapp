import torch

model_path = 'C:/dev2/dippa2/android/app/src/main/assets/summingModel.pt'
try:
    model = torch.jit.load(model_path)
    print(model)
    print("Model loaded successfully")
except Exception as e:
    print("Error loading the model:", e)


