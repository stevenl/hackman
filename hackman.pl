#!/usr/bin/env perl

use strict;
use warnings;
use Hackman::Bot;

# __main__
sub main {
    my $bot = Hackman::Bot->new;
    while (1) {
        $bot->update_game_state;
        #warn "\n", $bot->field->as_string, "\n";
        $bot->do_move;
    }
}

main();
