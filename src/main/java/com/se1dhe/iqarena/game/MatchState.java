package com.se1dhe.iqarena.game;

// Состояния server-authoritative матча.
public enum MatchState {
    match_found,
    round_open,
    round_reveal,
    match_complete,
    aborted
}
