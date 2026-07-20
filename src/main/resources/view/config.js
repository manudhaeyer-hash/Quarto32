import { GraphicEntityModule } from './entity-module/GraphicEntityModule.js';

import { TooltipModule } from './tooltip-module/TooltipModule.js';

// GraphicEntityModule : rend tout ce que crée le GraphicEntityModule Java.
export const modules = [
    GraphicEntityModule,
    TooltipModule
];

// Couleurs joueurs épinglées : doivent correspondre à Viewer.PLAYER_COLORS
// (indispensable si vous teintez des sprites avec setTint).
export const playerColors = [
    '#ff1d5c', // radical red
    '#22a1e4', // curious blue
    '#ff8f16', // west side orange
    '#6ac371'  // mantis green
];

// TODO: identifiant unique de votre jeu (cache des options du viewer en ligne).
export const gameName = 'Quarto';
