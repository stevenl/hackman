#!/usr/bin/env perl

use strict;
use warnings;

use Data::Dumper;
use Hackman::Game;
use Hackman::Point;

my @dx = (-1, 0, 1, 0);
my @dy = (0, -1, 0, 1);
my @moves = qw/up left down right/;

sub do_move {
    my ($game) = @_;
    my $me     = $game->player;
    my $field  = $game->field;

    my @valid_moves;
    for my $dir (0 .. 3) {
        my $nextx = $me->x + $dx[$dir];
        my $nexty = $me->y + $dy[$dir];
        my $next  = Hackman::Point->new(x => $nextx, y => $nexty);

        if ( $field->is_valid($next) && !$field->is_wall($next) ) {
            push @valid_moves, $moves[$dir];
        }
    }
    push @valid_moves, 'pass' if @valid_moves == 0;

    my $valid_move = $valid_moves[rand(scalar @valid_moves)];
    print "$valid_move\n";
};

# --------------------------------

# __main__
sub main {
    my $game = Hackman::Game->new;
    while (1) {
        $game->update_state;
        do_move($game);
    }
}
main();
