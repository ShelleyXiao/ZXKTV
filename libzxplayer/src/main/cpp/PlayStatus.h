
#ifndef PLAYER_PLAYSTATUS_H
#define PLAYER_PLAYSTATUS_H


class PlayStatus {

public:
    bool exit;
    bool pause;
    bool load;
    bool seek;

public:
    PlayStatus();
    ~PlayStatus();

};


#endif //PLAYER_PLAYSTATUS_H
