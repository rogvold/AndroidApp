function displayMessage(msg) {
	Android.showMessage(msg);
}

function startActivity(className) {
	Android.startActivity('com.cardiomood.android.' + className);
}