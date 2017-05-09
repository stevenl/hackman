#!/usr/bin/env perl

use strict;
use warnings;

use Data::Dumper;
use Hackman::Game;
use Hackman::Path;
use Hackman::Point;

# __main__
sub main {
    my $game = Hackman::Game->new;
    while (1) {
        $game->update_state;
        warn "\n", $game->field->as_string, "\n";
        do_move($game);
    }
}

sub do_move {
    my ($game) = @_;
    my $me     = $game->player;
    my $enemy  = $game->enemy;
    my $field  = $game->field;

    my @paths_to_items   = get_paths_to_items($game);
    my @paths_to_threats = filter_paths_to_threats($game, @paths_to_items);
    #local $Data::Dumper::Maxdepth = 2;
    #warn Dumper [map { $_->end } @paths_to_items];

    my %move_risk;
    if (!$me->has_weapon) {
        %move_risk = get_move_risk(@paths_to_threats);
    }
    warn Dumper \%move_risk;

    # Evasive action
    my $nr_immediate_risks = grep { $_ >= 4 } values %move_risk;
    if ($nr_immediate_risks > 1) {
        my @immediate_threats = @paths_to_threats[0 .. $nr_immediate_risks - 1];
        my ($path) = get_escape_path($game, @immediate_threats);
        # warn Dumper $path;
        print $path->first_move, "\n";
        return;
    }

    my @targets = grep {
        $field->has_snippet($_->end) || $field->has_weapon($_->end) || 
        ($me->has_weapon && $enemy->point == $_->end)
    } @paths_to_items;

    for my $target (@targets) {
        my $risk = $move_risk{ $target->first_move } // 0;

        my $move;
        if ($risk == 5) {
            next;
        } elsif ($risk == 4) {
            $move = $target->first_move if $target->nr_moves < 2;
        } elsif ($risk == 3) {
            $move = $target->first_move if $target->nr_moves < 4;
        } else {
            $move = $target->first_move;
        }

        if ($move) {
            print "$move\n";
            return;
        }
    }

    # Find alternate path
    my $move;
    $move //= 'pass';

    print "$move\n";
    return;
};

sub get_paths_to_items {
    my ($game) = @_;

    my $field = $game->field;
    my $start = $game->player->point;

    my @paths;
    my @queue   = ( Hackman::Path->new(start => $start) );
    my %visited = ( $start->as_string => 1 );

    while (@queue > 0) {
        my $path = shift @queue;

        for my $d (Hackman::Path->DIRECTIONS) {
            my $next = $path->add_move($d);

            next if $visited{ $next->end->as_string } ||
                $field->is_wall( $next->end ) ||
                !$field->is_valid( $next->end );

            if ( $field->has_item($next->end) ) {
                push @paths, $next;
                # return @paths if $n && @paths == $n;
            }

            $visited{ $next->end->as_string } = 1;
            push @queue, $next;
        }
    }

    return @paths;
}

sub filter_paths_to_threats {
    my ($game, @paths_to_items) = @_;

    my @threats = $game->field->bugs;

    my $enemy = $game->enemy;
    push @threats, $enemy if $enemy->has_weapon;

    my %item_path = map { $_->end->as_string => $_ } @paths_to_items;
    my @paths_to_threats = map $item_path{ $_->as_string }, @threats;

    return @paths_to_threats;
}

sub get_move_risk {
    my (@paths_to_threats) = @_;

    my %risk;
    for my $p (@paths_to_threats) {
        my $direction = $p->first_move;
        my $risk_level =
            $p->nr_moves > 16 ? 1 : # 16-
            $p->nr_moves > 8  ? 2 : # 9-16
            $p->nr_moves > 4  ? 3 : # 5-8
            $p->nr_moves > 2  ? 4 : # 3-4
            5;                      # 1-2

        if (!$risk{$direction} || $risk{$direction} < $risk_level) {
           $risk{$direction} = $risk_level;
        }
    }
    return %risk;
}

sub get_escape_path {
    my ($game, @threats) = @_;

    my $field = $game->field;
    my $start = $game->player->point;

    my @queue   = ( Hackman::Path->new(start => $start) );
    my %visited = ( $start->as_string => 1 );
    my %threat  = map { $_->end->as_string => 1 } @threats;

    while (@queue > 0 && keys %threat > 0) {
        my $path = shift @queue;

        for my $d (Hackman::Path->DIRECTIONS) {
            my $next = $path->add_move($d);

            next if $visited{ $next->end->as_string } ||
                $field->is_wall( $next->end ) ||
                !$field->is_valid( $next->end );

            next if delete $threat{ $next->end->as_string };

            $visited{ $next->end->as_string } = 1;
            push @queue, $next;
        }
    }

    return @queue;
}

main();
