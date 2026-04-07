// This is an example script for the HPVR controller devices, showing how to send OSC messages to VRC using the builtin VRChat integration.

HPVR.onButton(0, state => {
    console.info(`Button 0 changed to ${state}`);

    // this adds /avatar/parameters/ prefix, as a shortcut.
    const parStr = OSC.avtrParam("HPVR/B0");

    OSC.start().addBool(parStr, state).send();
});

// State persists between button presses
// Script gets compiled when device connects to the server app, and dies when device disconnects.
let change2 = 0;
const values = [0, 1, 2];
HPVR.onButton(1, state => {
    const res = state ? values[(change2++) % values.length] : 0;
    OSC.start().addInt(OSC.avtrParam("HPVR/I2"), res).send();
});

// Here's a more complex example which queries currently worn avatar and searches through parameters
HPVR.onButton(2, state => {
    const avatar = VRC.avatar;
    if (!avatar.loaded) return;

    const payload = OSC.start();

    // console.info(`Avatar "${avatar.name}" (${avatar.id}) is loaded, send custom parameter`);

    const param = avatar.parameters.find(p =>
        /^VF\d+_/.test(p.name) // VRCFury changes the menu components when you make exclusive tags. This example handles prefix VF000_
        && p.name.includes("My Menu Item")
        && p.canUpdate()
    );

    if (param) {
        // In this case, param is not just a string, but an object with more info about the parameter.
        // You can use it to check the type and send the correct value.
        // payload.addBool/Int/Float will automatically convert the value to the correct type based on the parameter, but you can also use addInt/Float directly if you want to send a specific type.
        // The (very important) note is that the param.name may contain spaces, while the actual address can not.
        // Pass the object itself for the server to get the correct OSC address.
        payload.addFloat(param, state ? 1.0 : 0.0);
    }

    payload.send();
});