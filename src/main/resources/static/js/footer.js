document.addEventListener("DOMContentLoaded", function () {
    document.body.classList.remove("no-transition");

    const toggleButton = document.getElementById("darkModeToggle");

    if (!toggleButton) {
        console.error("Dark Mode Toggle button not found!");
        return;
    }

    toggleButton.addEventListener("click", function () {
        console.log("Dark Mode button clicked!");

        document.body.classList.toggle("dark-mode");
        const darkModeEnabled = document.body.classList.contains("dark-mode");
        localStorage.setItem("darkMode", darkModeEnabled ? "true" : "false");

        toggleButton.textContent = darkModeEnabled ? "Light Mode" : "Dark Mode";
    });

    if (localStorage.getItem("darkMode") === "true") {
        document.body.classList.add("dark-mode");
        toggleButton.textContent = "Light Mode";
    } else {
        toggleButton.textContent = "Dark Mode";
    }
});
