const pageTitle = document.querySelector("#page-title");
const message = document.querySelector("#message");
const accountPanel = document.querySelector("#account-panel");
const accountForm = document.querySelector("#account-form");
const username = document.querySelector("#username");
const password = document.querySelector("#password");
const accountSubmit = document.querySelector("#account-submit");
const switchMode = document.querySelector("#switch-mode");
const logout = document.querySelector("#logout");
const profilePanel = document.querySelector("#profile-panel");
const profileEmpty = document.querySelector("#profile-empty");
const profileView = document.querySelector("#profile-view");
const profileForm = document.querySelector("#profile-form");
const savedPhone = document.querySelector("#saved-phone");
const savedAddress = document.querySelector("#saved-address");
const phoneNumber = document.querySelector("#phone-number");
const address = document.querySelector("#address");

let isRegistering = false;
let profile = null;
let busy = false;

function showAccountForm(register) {
    isRegistering = register;
    accountPanel.hidden = false;
    profilePanel.hidden = true;
    logout.hidden = true;
    pageTitle.textContent = register ? "Create account" : "Log in";
    accountSubmit.textContent = pageTitle.textContent;
    switchMode.textContent = register ? "Back to log in" : "Create an account";
    password.value = "";
    password.minLength = register ? 8 : 0;
    password.autocomplete = register ? "new-password" : "current-password";
    document.querySelector("#username-hint").hidden = !register;
    document.querySelector("#password-hint").hidden = !register;
}

function showProfile(details) {
    profile = details;
    accountPanel.hidden = true;
    profilePanel.hidden = false;
    logout.hidden = false;
    pageTitle.textContent = "Contact details";
    profileEmpty.hidden = details !== null;
    profileView.hidden = details === null;
    profileForm.hidden = true;
    savedPhone.textContent = details?.phoneNumber ?? "";
    savedAddress.textContent = details?.address ?? "";
}

function editProfile() {
    message.textContent = "";
    profileEmpty.hidden = true;
    profileView.hidden = true;
    profileForm.hidden = false;
    phoneNumber.value = profile?.phoneNumber ?? "";
    address.value = profile?.address ?? "";
    phoneNumber.focus();
}

async function loadProfile() {
    const response = await fetch("/profile");
    if (response.status === 401) {
        showAccountForm(false);
    } else if (response.status === 404) {
        showProfile(null);
    } else if (response.ok) {
        showProfile(await response.json());
    } else {
        throw new Error("Could not load your profile. Reload the page to try again.");
    }
}

async function sendChange(path, method, data) {
    // The browser sends the session cookie. Spring also requires a token for changes.
    const tokenResponse = await fetch("/csrf");
    if (!tokenResponse.ok) {
        throw new Error("Could not start the request. Try again.");
    }

    const csrf = await tokenResponse.json();
    const headers = { [csrf.headerName]: csrf.token };
    let body = data;
    if (data && !(data instanceof URLSearchParams)) {
        headers["Content-Type"] = "application/json";
        body = JSON.stringify(data);
    }
    return fetch(path, { method, headers, body });
}

async function runRequest(action) {
    if (busy) {
        return;
    }

    busy = true;
    const buttons = document.querySelectorAll("button");
    buttons.forEach(button => button.disabled = true);
    message.textContent = "";

    try {
        await action();
    } catch (error) {
        message.textContent = error instanceof TypeError
            ? "Could not connect. Check that the app is running and try again."
            : error.message;
    } finally {
        busy = false;
        buttons.forEach(button => button.disabled = false);
    }
}

async function changeProfile(method, details) {
    const response = await sendChange("/profile", method, details);
    if (response.status === 401) {
        showAccountForm(false);
        message.textContent = "Log in again to continue.";
        return;
    }
    if (response.status === 404 || response.status === 409) {
        await loadProfile();
        message.textContent = "Your profile changed. Review your details and try again.";
        return;
    }
    if (!response.ok) {
        throw new Error("Could not save the change. Try again.");
    }

    showProfile(method === "DELETE" ? null : await response.json());
    message.textContent = method === "DELETE" ? "Details deleted." : "Details saved.";
    pageTitle.focus();
}

switchMode.addEventListener("click", () => {
    message.textContent = "";
    showAccountForm(!isRegistering);
    username.focus();
});

accountForm.addEventListener("submit", event => {
    event.preventDefault();
    runRequest(async () => {
        const credentials = { username: username.value, password: password.value };
        if (isRegistering) {
            const response = await sendChange("/register", "POST", credentials);
            if (response.status === 409) {
                throw new Error("That username is already taken.");
            }
            if (!response.ok) {
                throw new Error("Could not create your account. Check your details and try again.");
            }
            showAccountForm(false);
            message.textContent = "Account created. Log in with your new password.";
            password.focus();
            return;
        }

        // Spring Security's login handler reads form fields.
        const response = await sendChange("/login", "POST", new URLSearchParams(credentials));
        if (response.status === 401) {
            throw new Error("Check your username and password.");
        }
        if (!response.ok) {
            throw new Error("Could not log in. Try again.");
        }
        accountForm.reset();
        await loadProfile();
        pageTitle.focus();
    });
});

document.querySelector("#add-profile").addEventListener("click", editProfile);
document.querySelector("#edit-profile").addEventListener("click", editProfile);
document.querySelector("#cancel-edit").addEventListener("click", () => {
    message.textContent = "";
    showProfile(profile);
    pageTitle.focus();
});

profileForm.addEventListener("submit", event => {
    event.preventDefault();
    const details = { phoneNumber: phoneNumber.value.trim(), address: address.value.trim() };
    if (!details.phoneNumber || !details.address) {
        message.textContent = "Enter a phone number and address.";
        return;
    }
    runRequest(() => changeProfile(profile === null ? "POST" : "PUT", details));
});

document.querySelector("#delete-profile").addEventListener("click", () => {
    if (window.confirm("Delete your phone number and address?")) {
        runRequest(() => changeProfile("DELETE"));
    }
});

logout.addEventListener("click", () => {
    runRequest(async () => {
        const response = await sendChange("/logout", "POST");
        if (!response.ok) {
            throw new Error("Could not log out. Try again.");
        }
        profileForm.reset();
        showProfile(null);
        showAccountForm(false);
        username.focus();
    });
});

runRequest(loadProfile);
