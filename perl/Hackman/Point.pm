package Hackman::Point;

use Moose;
use namespace::autoclean;

use overload (
    '""' => \&as_string,
    '==' => \&is_equal,
);

has x => (
    is       => 'ro',
    isa      => 'Int',
    required => 1,
);

has y => (
    is       => 'ro',
    isa      => 'Int',
    required => 1,
);

sub is_equal {
    my ($a, $b) = @_;
    return $a->x == $b->x && $a->y == $b->y;
}

sub as_string {
    my ($self) = @_;
    return sprintf '%d,%d', $self->x, $self->y;
}

my %delta = (
    up    => [-1, 0], 
    down  => [+1, 0],
    left  => [0, -1],
    right => [0, +1],
);

sub move {
    my ($self, $direction) = @_;

    my $delta = $delta{$direction};
    my $class = $self->meta->name;

    return $class->new(
        x => ($self->x + $delta->[0]),
        y => ($self->y + $delta->[1]),
    );
}

__PACKAGE__->meta->make_immutable;
1;
