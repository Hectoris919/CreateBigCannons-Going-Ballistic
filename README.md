<div align="center">
  <img width="256" height="256" alt="Icon" src="icon.png" />
  <h1>Create Big Cannons: Going Ballistic</h1>
</div>

Create Big Cannons: Going Ballistic is a mod that significantly increasing the range and speed of Create: Big Cannons projectiles by changing the formula used for velocity calculation to the one [Benjamin Robins developed](https://www.arc.id.au/CannonBallistics.html) in 1742:

$$
\begin{flalign}
\textbf{Robins' Cannon Formula}\\
v=1991 \sqrt{ \frac{p}{m + \frac{1}{3} p} \ln{ \left(\frac{L}{c}\right) } }
\end{flalign}
$$

This formula requires projectiles to have mass, so I estimated the weight of every projectile by assuming that the solid shot was entirely made of iron, calculating its volume, then basing the weight of the other projectiles off of the amount of iron items in their recipies. The machine gun round's mass looked similar to a 7.62x51mm / .308 round, just wider, so I took that round's mass and scaled it up a bit:

| Shell | Mass |
|-------|-----------|
|Solid Shot|3519.5 kg|
|AP Shot|3455.5 kg|
|Shrapnel Shell|3410.6 kg|
|AP Shell|3159.9 kg|
|HE Shell|2922.4 kg|
|Fluid Shell|2400.0 kg|
|Drop Mortar Shell|2255.5 kg|
|Mortar Stone|1162.3 kg|
|Smoke Shell|1037.0 kg|
|Grapeshot Shell|731.1 kg|
|Autocannon Shell (Flak)|32.1 kg|
|Autocannon Shell (AP)|33.9 kg|
|Machine Gun Bullet|0.012 kg|

## TODO:
- [x] Implement the base mod
- [ ] Add a ponder animation to the Powder Charge that shows how a cannon's size, amount of propellant, and shell type affects the projectile's trajectory
