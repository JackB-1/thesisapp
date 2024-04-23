const mapNumberToLabel = (numberString) => {
    const labels = ["acc_x", "acc_y", "acc_z", "gyro_x", "gyro_y", "gyro_z"];
    const index = parseInt(numberString, 10);
    if (index >= 0 && index < labels.length) {
        return labels[index];
    }
    throw new Error(`Invalid numberString: ${numberString}`);
};

export default mapNumberToLabel;