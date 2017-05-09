package Hackman::Bot;

use Moose;
use namespace::autoclean;

use Data::Dumper;
use Hackman::Path;
use Hackman::Point;

with 'Hackman::Game';

sub do_move {
    my ($self) = @_;
    my $move = $self->_determine_move // 'pass';
    print "$move\n";
    return;
}

sub _determine_move {
    my ($self) = @_;
    my $me     = $self->player;
    my $enemy  = $self->enemy;
    my $field  = $self->field;

    my @paths;
    if ($field->nr_artifacts == 0) {
        #return $self->_random_move;
        @paths = $self->_find_safe_paths;
    } else {
        if (!$me->has_weapon) {
            @paths = $self->_find_shortest_paths('avoid_threats');
            #warn Dumper \@paths;
        }
        if (!@paths) {
            @paths = $self->_find_shortest_paths;
        }
    }

    my $move;
    $move = $paths[0]->first_move if @paths;
    return $move;
}

# Do a breadth-first search to find the shortest path to each artifact.
# Returns a list of paths, ordered by increasing distance.
sub _find_shortest_paths {
    my ($self, $safe_mode) = @_;

    my $field = $self->field;
    my $start = $self->player->point;

    my @queue   = ( Hackman::Path->new(start => $start) );
    my %visited = ( $start->as_string => 1 );

    if ($safe_mode) {
        $visited{ $_->as_string } = 1 for $self->_threats;
    }

    my @paths;
    while (@queue > 0) {
        my $path = shift @queue;

        for my $d (Hackman::Path->DIRECTIONS) {
            my $next = $path->add_move($d);

            next if $visited{ $next->end->as_string } ||
                $field->is_wall( $next->end ) ||
                !$field->is_valid( $next->end );

            if ( $field->has_artifact($next->end) ) {
                push @paths, $next;
            }

            $visited{ $next->end->as_string } = 1;
            push @queue, $next;
        }
    }
    return @paths;
}

sub _find_safe_paths {
    my ($self, $max_iterations) = @_;
    $max_iterations //= 10;

    my $field = $self->field;
    my $start = $self->player->point;

    my @queue   = ( Hackman::Path->new(start => $start) );
    my %visited = ( $start->as_string => 1 );
    my %threat  = map { $_->as_string => 1 } $self->_threats;

    my $i = 0;
    while ($i++ < $max_iterations && @queue > 0 && keys %threat > 0) {
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

sub _threats {
    my ($self) = @_;

    my @threats = $self->field->bugs;

    my $enemy = $self->enemy;
    push @threats, $enemy->point if $enemy->has_weapon;

    return @threats;
}

__PACKAGE__->meta->make_immutable;
1;
